package fun.linkify.backend;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BackendController {

    private ModelAndView modelAndView = new ModelAndView("index");

    private UrlValidator urlValidator = new UrlValidator();

    private Base64 base64 = new Base64();

    private FirestoreOptions firestoreOptions =
            FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(System.getenv("GCLOUD_PROJECT"))
                    .build();

    private Firestore db = firestoreOptions.getService();

    private URL baseUrl = new URL("https://linkify.fun/");

    public BackendController() throws MalformedURLException { }

    @RequestMapping(path = "/", method = GET)
    public ModelAndView index () {
        return modelAndView;
    }

    @RequestMapping(path = "/", method = POST)
    public ResponseEntity<Map<String, String>> shorten(@RequestParam String url) throws ExecutionException, InterruptedException, MalformedURLException {

        if (StringUtils.isEmpty(url) || !urlValidator.isValid(url))
            return ResponseEntity.badRequest().build();

        String digested_url = DigestUtils.sha256Hex(url);

        String encoded_url = new String(base64.encode(digested_url.getBytes()));

        String ID = encoded_url.substring(0, 8);

        DocumentReference docRef = db.collection("urls").document(ID);

        Map<String, Object> data = Collections.singletonMap("url", url);

        ApiFuture<WriteResult> writeResultApiFuture = docRef.set(data);

        writeResultApiFuture.get();

        URL short_url = new URL(baseUrl, ID);

        return ResponseEntity.ok().body(Collections.singletonMap("short_url", short_url.toString()));

    }

    @RequestMapping(path = "/{ID}", method = GET)
    public ResponseEntity<Object> getBack(@PathVariable String ID) throws ExecutionException, InterruptedException, URISyntaxException {

        DocumentReference docRef = db.collection("urls").document(ID);

        ApiFuture<DocumentSnapshot> future = docRef.get();

        DocumentSnapshot document = future.get();

        if (!document.exists())
            return ResponseEntity.notFound().build();

        Map<String, Object> data = document.getData();

        if (data == null || !data.containsKey("url"))
            return ResponseEntity.notFound().build();

        String url = String.valueOf(data.get("url"));

        URI uri = new URI(url);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(uri);

        return new ResponseEntity<>(httpHeaders, HttpStatus.SEE_OTHER);

    }

}
