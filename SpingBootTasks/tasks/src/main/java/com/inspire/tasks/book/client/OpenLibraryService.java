package com.inspire.tasks.book.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Map;

public interface OpenLibraryService {

    @GetExchange("/search.json")
    String findBookByName(@RequestParam("q") String bookName);

    @GetExchange("/works/{id}.json")
    Map<String, Object> getWorkById(@PathVariable("id") String workId);
}
