package cl.sustentia.apisales;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Plan {
    private final String id;
    private final int maxProducts;
    private final int maxAnnouncements;

    public Plan(String id, int maxProducts, int maxAnnouncements) {
        this.id = id;
        this.maxProducts = maxProducts;
        this.maxAnnouncements = maxAnnouncements;
    }

    public String getId() {
        return id;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public int getMaxAnnouncements() {
        return maxAnnouncements;
    }
}
