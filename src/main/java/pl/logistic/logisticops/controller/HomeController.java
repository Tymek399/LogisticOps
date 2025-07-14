package pl.logistic.logisticops.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return """
            <h1>🚛 LogisticOps - System Logistyczny</h1>
            <p>Backend is running!</p>
            <ul>
                <li><a href="/api/dashboard">📊 Dashboard API</a></li>
                <li><a href="/api/transports">🚚 Transporty API</a></li>
                <li><a href="/api/missions">🎖️ Misje API</a></li>
                <li><a href="/api/vehicles">🛡️ Pojazdy API</a></li>
                <li><a href="/api/infrastructure">🌉 Infrastruktura API</a></li>
                <li><a href="/ws">🔌 WebSocket Endpoint</a></li>
                <li><a href="/actuator/health">💚 Health Check</a></li>
            </ul>
            <p><strong>Frontend:</strong> Otwórz plik index.html w przeglądarce</p>
            """;
    }

    @GetMapping("/status")
    @ResponseBody
    public String status() {
        return "LogisticOps Backend - OK ✅";
    }
}