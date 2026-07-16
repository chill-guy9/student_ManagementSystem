package com.sms.nexus.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long pageSize;
    private long pages;

    public PageResult() {}

    public PageResult(List<T> records, long total, long page, long pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = (total + pageSize - 1) / pageSize;
    }
}
