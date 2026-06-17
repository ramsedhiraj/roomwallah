package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;

public interface AccountLifecycleService {
    void deactivateAccount(User user);
}
