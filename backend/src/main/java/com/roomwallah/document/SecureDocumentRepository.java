package com.roomwallah.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecureDocumentRepository extends JpaRepository<SecureDocument, UUID> {
    List<SecureDocument> findByOwnerIdAndIsDeletedFalse(UUID ownerId);
}
