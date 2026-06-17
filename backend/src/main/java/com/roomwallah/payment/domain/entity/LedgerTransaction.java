package com.roomwallah.payment.domain.entity;

import com.roomwallah.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ledger_transactions")
@Getter
@Setter
@ToString(callSuper = true, exclude = "entries")
@EqualsAndHashCode(callSuper = true, exclude = "entries")
public class LedgerTransaction extends BaseEntity {

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> entries = new ArrayList<>();

    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
        entry.setTransaction(this);
    }

    public void removeEntry(LedgerEntry entry) {
        entries.remove(entry);
        entry.setTransaction(null);
    }
}
