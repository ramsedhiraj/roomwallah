package com.roomwallah.identity.application.service;

import com.roomwallah.user.entity.User;

public interface CurrentUserProvider {
    User getCurrentUser();
}
