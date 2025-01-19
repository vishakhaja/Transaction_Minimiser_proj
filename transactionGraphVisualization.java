import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class TransactionGraphVisualization {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input number of people
        System.out.print("Enter the number of people: ");
        int numPeople = sc.nextInt();
        sc.nextLine(); // Consume newline

        // Input names of people
        System.out.print("Enter the names of the people (separated by spaces): ");
        String[] people = sc.nextLine().split(" ");
        if (people.length != numPeople) {
            System.out.println("Error: Number of names does not match the number of people.");
            return;
        }

        // Initialize balances
        Map<String, Integer> balances = new HashMap<>();
        for (String person : people) {
            balances.put(person, 0);
        }

        // Input transactions
        System.out.println("Enter transactions in the form {giver taker amount} (type STOP to finish):");
        ArrayList<String[]> transactions = new ArrayList<>();
        while (true) {
            String input = sc.nextLine();
            if (input.equalsIgnoreCase("STOP")) {
                break;
            }

            String[] transaction = input.split(" ");
            if (transaction.length != 3) {
                System.out.println("Error: Invalid transaction format. Please use {giver taker amount}.");
                continue;
            }

            String giver = transaction[0];
            String taker = transaction[1];
            int amount;

            try {
                amount = Integer.parseInt(transaction[2]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Amount must be an integer.");
                continue;
            }

            // Validate participants
            if (!balances.containsKey(giver)) {
                System.out.println("Error: " + giver + " is not part of the initial list of people.");
                continue;
            }
            if (!balances.containsKey(taker)) {
                System.out.println("Error: " + taker + " is not part of the initial list of people.");
                continue;
            }

            // Update balances
            balances.put(giver, balances.get(giver) - amount);
            balances.put(taker, balances.get(taker) + amount);

            // Add the transaction to the list
            transactions.add(transaction);
        }

        // Minimize the transactions
        ArrayList<Map.Entry<String, Integer>> creditors = new ArrayList<>();
        ArrayList<Map.Entry<String, Integer>> debtors = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : balances.entrySet()) {
            if (entry.getValue() > 0) {
                creditors.add(entry); // Collect creditors
            } else if (entry.getValue() < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), -entry.getValue())); // Collect debtors
            }
        }

        // Create a graph for the original transactions
        mxGraph graphBefore = new mxGraph();
        Object parentBefore = graphBefore.getDefaultParent();
        graphBefore.getModel().beginUpdate();
        Map<String, Object> verticesBefore = new HashMap<>();
        try {
            // Add vertices (people)
            for (String person : balances.keySet()) {
                verticesBefore.put(person, graphBefore.insertVertex(parentBefore, null, person, 100, 100, 80, 30));
            }

            // Add edges for original transactions
            for (String[] transaction : transactions) {
                String giver = transaction[0];
                String taker = transaction[1];
                int amount = Integer.parseInt(transaction[2]);
                graphBefore.insertEdge(parentBefore, null, giver + " pays " + amount + " to " + taker,
                        verticesBefore.get(giver), verticesBefore.get(taker));
            }
        } finally {
            graphBefore.getModel().endUpdate();
        }

        // Create a graph for the minimized transactions
        mxGraph graphAfter = new mxGraph();
        Object parentAfter = graphAfter.getDefaultParent();
        graphAfter.getModel().beginUpdate();
        Map<String, Object> verticesAfter = new HashMap<>();
        try {
            // Add vertices (people)
            for (String person : balances.keySet()) {
                verticesAfter.put(person, graphAfter.insertVertex(parentAfter, null, person, 100, 100, 80, 30));
            }

            // Add edges for minimized transactions (settlements)
            int i = 0, j = 0;
            while (i < creditors.size() && j < debtors.size()) {
                Map.Entry<String, Integer> creditor = creditors.get(i);
                Map.Entry<String, Integer> debtor = debtors.get(j);

                int settlementAmount = Math.min(creditor.getValue(), debtor.getValue());
                graphAfter.insertEdge(parentAfter, null, creditor.getKey() + " pays " + settlementAmount + " to " + debtor.getKey(),
                        verticesAfter.get(creditor.getKey()), verticesAfter.get(debtor.getKey()));

                // Update balances
                creditor.setValue(creditor.getValue() - settlementAmount);
                debtor.setValue(debtor.getValue() - settlementAmount);

                // Remove settled creditors/debtors
                if (creditor.getValue() == 0) {
                    i++;
                }
                if (debtor.getValue() == 0) {
                    j++;
                }
            }
        } finally {
            graphAfter.getModel().endUpdate();
        }

        // Step 6: Create layouts for both graphs
        mxCircleLayout layoutBefore = new mxCircleLayout(graphBefore);
        layoutBefore.execute(parentBefore);

        mxCircleLayout layoutAfter = new mxCircleLayout(graphAfter);
        layoutAfter.execute(parentAfter);

        // Step 7: Create graph components for displaying both graphs
        mxGraphComponent graphComponentBefore = new mxGraphComponent(graphBefore);
        mxGraphComponent graphComponentAfter = new mxGraphComponent(graphAfter);

        // Step 8: Create the frame for displaying both graphs
        JFrame frame = new JFrame("Transaction Graphs");
        frame.setLayout(new GridLayout(1, 2)); // Display graphs side by side
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().add(graphComponentBefore); // Add original transactions graph
        frame.getContentPane().add(graphComponentAfter);  // Add minimized transactions graph

        frame.setSize(1600, 600); // Adjust the frame size
        frame.setVisible(true);

        sc.close();
    }
}
