package com.roomwallah.search.domain.model;

import lombok.Value;

@Value
public class CursorPage {
    String cursor;
    int size;
}
