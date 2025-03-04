package com.example.UrlShortener;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;


@RestController
@RequestMapping("/api/url")
public class UrlShortenerController {

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Autowired
    private RateLimiter rateLimiter;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody String originalUrl, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (!rateLimiter.allowRequest(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }

        String shortUrl = urlShortenerService.shortenUrl(originalUrl);
        return ResponseEntity.ok("http://short.url/" + shortUrl);
    }

    @GetMapping("/{shortUrl}")
    public RedirectView redirectUrl(@PathVariable String shortUrl, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String originalUrl = urlShortenerService.getOriginalUrl(shortUrl);
        String server = urlShortenerService.getServerForShortUrl(shortUrl);

        if (originalUrl == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Short URL not found");
            return null;
        }

        String currentServer = determineCurrentServer(request); // Determine the current server dynamically
        if (server != null && !server.isEmpty() && !currentServer.equals(server)) {
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl("http://" + server + "/" + shortUrl);
            return redirectView;
        }

        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(originalUrl);
        return redirectView;
    }

    private String determineCurrentServer(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host != null && !host.isEmpty()) {
            return host;
        } else {
            return request.getServerName();
        }
    }


//    @GetMapping("/{shortUrl}")
//    public ResponseEntity<String> getOriginalUrl(@PathVariable String shortUrl) {
//        String originalUrl = urlShortenerService.getOriginalUrl(shortUrl);
//        if (originalUrl != null) {
//            return ResponseEntity.ok(originalUrl);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
}
