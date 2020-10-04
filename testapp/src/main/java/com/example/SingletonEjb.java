package com.example;

import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class SingletonEjb {

    @Inject
    private MyStatelessEjb myStatelessEjb;

}
