package com.roomwallah.user.service;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.presentation.dto.AvatarUploadResponse;

import java.io.InputStream;

public interface AvatarService {
    AvatarUploadResponse uploadAvatar(User user, InputStream inputStream, String originalFileName, String contentType);
}
