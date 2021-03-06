/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.email.notifier;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.blackducksoftware.integration.email.extension.config.ExtensionInfo;
import com.blackducksoftware.integration.email.model.DateRange;
import com.blackducksoftware.integration.email.model.ExtensionProperties;
import com.blackducksoftware.integration.email.service.EmailMessagingService;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;

public class DailyDigestNotifier extends AbstractDigestNotifier {
    public DailyDigestNotifier(final ExtensionProperties customerProperties,
            final EmailMessagingService emailMessagingService, final HubServicesFactory hubServicesFactory, final ExtensionInfo extensionInfo) {
        super(customerProperties, emailMessagingService, hubServicesFactory, extensionInfo);
    }

    @Override
    public DateRange createDateRange() {
        final ZonedDateTime currentTime = ZonedDateTime.now();
        final ZoneId zone = currentTime.getZone();
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

    @Override
    public String createCronExpression() {
        // 12:00am UTC time.
        return "0 0 0 1/1 * ? *";
    }
}
