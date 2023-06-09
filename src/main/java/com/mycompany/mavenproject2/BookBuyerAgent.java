package com.mycompany.mavenproject2;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;

// Przykładowa klasa zachowania:
//class MyOwnBehaviour extends Behaviour
//{
//  protected MyOwnBehaviour()
//  {
//  }
//
//  public void action()
//  {
//  }
//  public boolean done() {
//    return false;
//  }
//}

public class BookBuyerAgent extends Agent {

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi) - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private final AID[] sellerAgents = {
            new AID("seller1", AID.ISLOCALNAME),
            new AID("seller2", AID.ISLOCALNAME)};

    // Inicjalizacja klasy agenta:
    protected void setup()
    {

        //doWait(6000);   // Oczekiwanie na uruchomienie agentów sprzedających

        System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja d <2020/21>) jest gotów!");

        Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

        if (args != null && args.length > 0)   // jeśli podano tytuł książki
        {
            targetBookTitle = (String) args[0];
            System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

//            addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań — kod znajduje się poniżej
            addBehaviour(new Zadanie3Behaviour());
        }
        else
        {
            // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
            System.out.println("Proszę podać tytuł lektury w argumentach wejściowych agenta kupującego!");
            doDelete();
        }
    }
    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
        System.out.println("Agent-kupiec "+getAID().getName()+" kończy.");
    }

    /**
     Inner class RequestPerformer.
     This is the behaviour used by Book-buyer agents to request seller
     agents the target book.
     */
    private class RequestPerformer extends Behaviour
    {

        private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
        private int bestPrice;      // najlepsza cena
        private int repliesCnt = 0; // liczba odpowiedzi od agentów
        private MessageTemplate mt; // szablon odpowiedzi
        private int step = 0;       // krok

        public void action()
        {
            ACLMessage reply;
            switch (step) {
                case 0 -> {      // wysłanie oferty kupna
                    System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
                    for (AID agent : sellerAgents) {
                        System.out.print(agent + " ");
                    }
                    System.out.println();

                    // Tworzenie wiadomości CFP do wszystkich sprzedawców:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID sellerAgent : sellerAgents) {
                        cfp.addReceiver(sellerAgent);                // dodanie adresata
                    }
                    cfp.setContent(targetBookTitle);                   // wpisanie zawartości — tytułu książki
                    cfp.setConversationId("handel_ksiazkami");         // wpisanie specjalnego identyfikatora korespondencji
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
                    myAgent.send(cfp);                           // wysłanie wiadomości


                    // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;     // przejście do kolejnego kroku
                }
                case 1 -> {      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
                        {
                            int price = Integer.parseInt(reply.getContent());  // cena książki
                            if (bestSeller == null || price < bestPrice)       // jeśli jest to najlepsza oferta
                            {
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;                                        // liczba ofert
                        if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
                        {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("handel_ksiazkami");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                }
                case 3 -> {      // odbiór odpowiedzi na zamówienie
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("Tytuł " + targetBookTitle + " zamówiony!");
                            System.out.println("Po cenie: " + bestPrice);
                            myAgent.doDelete();
                        }
                        step = 4;
                    } else {
                        block();
                    }
                }
            }
        }

        public boolean done() {
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }

    private class Zadanie3Behaviour extends Behaviour {

        private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
        private int bestPrice;      // najlepsza cena
        private int repliesCnt = 0; // liczba odpowiedzi od agentów
        private MessageTemplate mt; // szablon odpowiedzi
        private int step = 0;       // krok

        private final int maxNumberOfTries = 8;
        private int numberOfTries = 0;
        private final int increase = 6;
        private int lastProposedPrice = 0;

        @Override
        public void action() {
            ACLMessage reply;
            switch (step) {
                case 0 -> {      // wysłanie oferty kupna
                    System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
                    for (AID agent : sellerAgents) {
                        System.out.print(agent + " ");
                    }
                    System.out.println();

                    // Tworzenie wiadomości CFP do wszystkich sprzedawców:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID sellerAgent : sellerAgents) {
                        cfp.addReceiver(sellerAgent);                // dodanie adresata
                    }
                    cfp.setContent(targetBookTitle);                   // wpisanie zawartości — tytułu książki
                    cfp.setConversationId("handel_ksiazkami");         // wpisanie specjalnego identyfikatora korespondencji
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
                    myAgent.send(cfp);                           // wysłanie wiadomości


                    // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;     // przejście do kolejnego kroku
                }
                case 1 -> {      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
                        {
                            int price = Integer.parseInt(reply.getContent());  // cena książki
                            if (bestSeller == null || price < bestPrice)       // jeśli jest to najlepsza oferta
                            {
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;                                        // liczba ofert
                        if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
                        {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    int proposalPrice = (int) (bestPrice * 0.6);
                    lastProposedPrice = proposalPrice;
                    System.out.println("Początkowa oferta od kupca: " + proposalPrice);
                    ACLMessage initialProposal = new ACLMessage(ACLMessage.PROPOSE);
                    initialProposal.addReceiver(bestSeller);
                    initialProposal.setContent(Integer.toString(proposalPrice));
                    initialProposal.setConversationId("handel_ksiazkami");
                    initialProposal.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(initialProposal);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                            MessageTemplate.MatchInReplyTo(initialProposal.getReplyWith()));
                    step = 3;
                }
                case 3 -> {
                    reply = myAgent.receive(mt);
                    if (reply == null) return;

                    int proposedPrice = Integer.parseInt(reply.getContent());
                    if (Math.abs(proposedPrice - lastProposedPrice) <= 3) {
                        // bieremy
                        System.out.println("Tytuł " + targetBookTitle + " zamówiony!");
                        System.out.println("Po cenie: " + proposedPrice);

                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestSeller);
                        order.setContent(Integer.toString(proposedPrice));
                        order.setConversationId("handel_ksiazkami");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        myAgent.doDelete();
                        step = 4;
                        return;
                    }

                    if (numberOfTries++ >= maxNumberOfTries) {
                        ACLMessage order = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                        order.addReceiver(bestSeller);
                        order.setConversationId("handel_ksiazkami");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        myAgent.doDelete();
                        step = 4;
                    }

                    int newPrice = lastProposedPrice + increase;
                    lastProposedPrice = newPrice;
                    System.out.println("Oferta #" + numberOfTries + " od kupca, cena: " + newPrice);
                    ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
                    proposal.addReceiver(bestSeller);
                    proposal.setContent(Integer.toString(newPrice));
                    proposal.setConversationId("handel_ksiazkami");
                    proposal.setReplyWith("order" + System.currentTimeMillis());myAgent.send(proposal);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_ksiazkami"),
                            MessageTemplate.MatchInReplyTo(proposal.getReplyWith()));
                }
            }
        }

        @Override
        public boolean done() {
            return step == 4;
        }
    }
}
