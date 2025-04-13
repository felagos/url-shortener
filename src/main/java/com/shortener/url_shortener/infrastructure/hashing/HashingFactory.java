package com.shortener.url_shortener.infrastructure.hashing;

import java.util.Map;

public class HashingFactory {

    private static HashingFactory instance;
    private Map<HashingType, IHashing> hashers;

    private HashingFactory() {
        this.hashers = Map.of(
                HashingType.BASE62, new Base62(),
                HashingType.UNIQUE_ID, new UniqueId()
        );
    }

    public static HashingFactory getInstance() {
        if (instance == null) {
            instance = new HashingFactory();
        }
        return instance;
    }

    public IHashing getHasher(HashingType type) {
        return hashers.get(type);
    }

}
