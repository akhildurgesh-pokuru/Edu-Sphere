package com.edusphere.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/** File storage abstraction. The local-disk implementation can be swapped for Amazon S3 later. */
public interface StorageService {
    /** Stores the stream under a random server-chosen key and returns that key. */
    String store(InputStream in, String extension) throws IOException;

    Resource load(String key);

    void delete(String key);
}
