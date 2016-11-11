package com.blackducksoftware.integration.email.notifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.blackducksoftware.integration.email.model.DateRange;
import com.blackducksoftware.integration.email.model.ExtensionProperties;
import com.blackducksoftware.integration.email.service.EmailMessagingService;
import com.blackducksoftware.integration.hub.dataservices.DataServicesFactory;

public class DailyDigestNotifier extends AbstractDigestNotifier {

    public DailyDigestNotifier(final ExtensionProperties customerProperties,
            final EmailMessagingService emailMessagingService, final DataServicesFactory dataservicesFactory) {
        super(customerProperties, emailMessagingService, dataservicesFactory);
    }

    @Override
    public DateRange createDateRange(final ZoneId zone) {
        final LocalDateTime currentTime = LocalDateTime.now();
        final ZonedDateTime endZonedTime = ZonedDateTime.of(currentTime.getYear(), currentTime.getMonthValue(),
                currentTime.getDayOfMonth(), 23, 59, 59, 999, zone).minusDays(1);
        final ZonedDateTime startZonedTime = ZonedDateTime
                .of(currentTime.getYear(), currentTime.getMonthValue(), currentTime.getDayOfMonth(), 0, 0, 0, 0, zone)
                .minusDays(1);

        return new DateRange(Date.from(startZonedTime.toInstant()), Date.from(endZonedTime.toInstant()));
    }

    @Override
    public String getNotifierPropertyKey() {
        return "dailyDigest";
    }

    @Override
    public String getCategory() {
        return "Daily";
    }
}
