package com.safeanot.app.testutil;

import com.safeanot.app.data.local.ReminderConfigDao;
import com.safeanot.app.domain.repository.AuditRepository;
import com.safeanot.app.domain.usecase.GetAuditStatsUseCase;
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase;
import com.safeanot.app.domain.usecase.SetPreferredRegionUseCase;
import com.safeanot.app.feature.profile.ProfileViewModel;
import com.safeanot.app.worker.AuditReminderScheduler;

/**
 * Factory to create ProfileViewModel in tests, bypassing Kotlin null-safety
 * for AuditReminderScheduler's Context parameter.
 */
public final class TestProfileViewModelFactory {

    private TestProfileViewModelFactory() {}

    public static ProfileViewModel create(
            AuditRepository fakeAuditRepo,
            FakeUserPreferencesRepository fakePrefs
    ) {
        // Create a fake scheduler that passes null context (safe because all methods are overridden)
        AuditReminderScheduler fakeScheduler = new AuditReminderScheduler(null) {
            @Override
            public void schedule(int intervalDays) { /* no-op */ }

            @Override
            public void cancel() { /* no-op */ }

            @Override
            public void updateSchedule(boolean enabled, int intervalDays) { /* no-op */ }
        };

        // Create a fake ReminderConfigDao
        ReminderConfigDao fakeDao = new FakeReminderConfigDao();

        return new ProfileViewModel(
                fakeDao,
                fakeScheduler,
                new GetAuditStatsUseCase(fakeAuditRepo),
                new GetPreferredRegionUseCase(fakePrefs),
                new SetPreferredRegionUseCase(fakePrefs),
                fakePrefs
        );
    }
}
