package fun.linkify.backend;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class BackendController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Linkify!";
    }
}
