package com.example;


import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class EjbClient {
    @EJB
    private MyStatelessEjb myStatelessEjb;
}
