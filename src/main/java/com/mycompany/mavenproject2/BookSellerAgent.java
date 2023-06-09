package com.mycompany.mavenproject2;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import java.lang.*;

public class BookSellerAgent extends Agent
{
    // Tworzenie katalogu lektur jako tablicy rozproszonej
    // Katalog lektur na sprzedaż:
    private final Hashtable<String, Integer> catalogue = new Hashtable<>();

    // Inicjalizacja klasy agenta:
    protected void setup()
    {
        Random randomGenerator = new Random();    // generator liczb losowych

        catalogue.put("Zamek", 90+randomGenerator.nextInt(500));       // nazwa lektury jako klucz, cena jako wartość
        catalogue.put("Opowiadania", 110+randomGenerator.nextInt(200));
        catalogue.put("Ameryka", 300+randomGenerator.nextInt(70));
        catalogue.put("Proces", 250+randomGenerator.nextInt(250));

        doWait(2019);                     // czekaj 2 sekundy

        System.out.println("Witam! Agent-sprzedawca (wersja b <2022/23>) "+getAID().getName()+" jest gotów do działania!");

        // Dodanie zachowania obsługującego odpowiedzi na oferty klientów (kupujących książki):
        addBehaviour(new OfferRequestsServer());

        // Dodanie zachowania obsługującego zamówienie klienta:
        addBehaviour(new PurchaseOrdersServer());
    }

    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
        System.out.println("Agent-sprzedawca (wersja b <2022/23>) "+getAID().getName()+" zakończył się.");
    }

    private int currentProposalPrice = 0;
    private int lastProposalPrice = 0;
    private int getNextProposalPrice() {
        return (currentProposalPrice + lastProposalPrice) / 2;
    }


    /**
     Inner class OfferRequestsServer.
     This is the behaviour used by Book-seller agents to serve incoming requests
     for offer from buyer agents.
     If the requested book is in the local catalogue the seller agent replies
     with a PROPOSE message specifying the price. Otherwise a REFUSE message is sent back.
     */
    class OfferRequestsServer extends CyclicBehaviour
    {

        public void action()
        {
            // Tworzenie szablonu wiadomości (wstępne określenie tego, co powinna zawierać wiadomość)
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            // Próba odbioru wiadomości zgodnej z szablonem:
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {  // jeśli nadeszła wiadomość zgodna z ustalonym wcześniej szablonem
                String title = msg.getContent();  // odczytanie tytułu zamawianej książki

                System.out.println("Agent-sprzedawca  "+getAID().getName()+" otrzymał komunikat: "+
                        title);
                ACLMessage reply = msg.createReply();               // tworzenie wiadomości — odpowiedzi
                Integer price = catalogue.get(title);     // ustalenie ceny dla podanego tytułu
                currentProposalPrice = price;
                lastProposalPrice = currentProposalPrice;
                // jeśli taki tytuł jest dostępny
                reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
                reply.setContent(String.valueOf(price.intValue()));   // umieszczenie ceny w polu zawartości (content)
                System.out.println("Agent-sprzedawca " + getAID().getName() + " odpowiada: " + price);
                myAgent.send(reply);                                // wysłanie odpowiedzi
            }
            else                         // jeśli wiadomość nie nadeszła lub była niezgodna z szablonem
            {
                block();                   // blokada metody action() dopóki nie pojawi się nowa wiadomość
            }
        }
    }


    class PurchaseOrdersServer extends CyclicBehaviour
    {

        public void action()
        {
            ACLMessage msg = myAgent.receive();
            if (msg == null) return;

            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                int proposedPrice = Integer.parseInt(msg.getContent());
                System.out.println("Odebranie propozycji od kupca, proponowana cena: " + proposedPrice);
                lastProposalPrice = currentProposalPrice;
                currentProposalPrice = proposedPrice;
                int newPropose = getNextProposalPrice();
                lastProposalPrice = currentProposalPrice;
                currentProposalPrice = newPropose;
                System.out.println("Sprzedawca odpowiada z ceną: " + newPropose);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Integer.toString((newPropose)));
                myAgent.send(reply);
            }

            if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                System.out.println("nie dogadali się ://");

            }

            if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
            {
                // Message received. Process it
                ACLMessage reply = msg.createReply();
                String title = msg.getContent();
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println("Agent sprzedający (wersja d <2021/22>) "+getAID().getName()+" sprzedał książkę: "+title);
                myAgent.send(reply);
            }
        }
    }
}
