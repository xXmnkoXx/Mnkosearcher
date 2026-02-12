package com.ordenatec.portallicitaciones.api.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public class PublicadasResponse<T> {

    private Map<String, Object> params;
    private Page<T> page;

    public PublicadasResponse() {}

    public PublicadasResponse(Map<String, Object> params, Page<T> page) {
        this.params = params;
        this.page = page;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Page<T> getPage() {
        return page;
    }

    public void setPage(Page<T> page) {
        this.page = page;
    }
}
