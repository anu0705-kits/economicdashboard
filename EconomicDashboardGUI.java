import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

public class EconomicDashboardGUI extends JFrame {

    private List<EconomicData> dataList;
    private GraphPanel graphPanel;

    public EconomicDashboardGUI() {
        setTitle("Economic Research Dashboard");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        dataList = loadData();

        if (dataList.isEmpty()) {
            dataList = new ArrayList<>();
            dataList.add(new EconomicData(2018, 6.8, 3.5, 5.2));
            dataList.add(new EconomicData(2019, 7.0, 4.0, 5.0));
            dataList.add(new EconomicData(2020, -2.0, 6.2, 8.5));
            dataList.add(new EconomicData(2021, 8.5, 5.5, 6.0));
            dataList.add(new EconomicData(2022, 7.2, 6.8, 5.3));
            dataList.add(new EconomicData(2023, 6.2, 7.8, 8.3));
            dataList.add(new EconomicData(2024, 5.2, 9.8, 6.3));
            dataList.add(new EconomicData(2025, 6.2, 8.8, 7.3));
        }

        graphPanel = new GraphPanel(dataList);
        add(graphPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton searchButton = new JButton("Search Year");
        searchButton.addActionListener(e -> searchYearData());

        JButton addButton = new JButton("Add Data");
        addButton.addActionListener(e -> addNewData());

        buttonPanel.add(addButton);
        buttonPanel.add(searchButton);

        add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveData(dataList);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EconomicDashboardGUI().setVisible(true));
    }

    private void addNewData() {
        try {
            int year = Integer.parseInt(JOptionPane.showInputDialog("Enter Year"));
            double gdp = Double.parseDouble(JOptionPane.showInputDialog("Enter GDP"));
            double inflation = Double.parseDouble(JOptionPane.showInputDialog("Enter Inflation"));
            double unemployment = Double.parseDouble(JOptionPane.showInputDialog("Enter Unemployment"));

            dataList.add(new EconomicData(year, gdp, inflation, unemployment));
            graphPanel.updateData(dataList);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input!");
        }
    }

    private void searchYearData() {
        String input = JOptionPane.showInputDialog(this, "Enter Year to search:");
        if (input == null || input.isEmpty()) return;

        try {
            int year = Integer.parseInt(input);
            StringBuilder result = new StringBuilder();
            boolean found = false;

            for (EconomicData d : dataList) {
                if (d.year == year) {
                    result.append("Year: ").append(d.year)
                          .append("\nGDP: ").append(d.gdp)
                          .append("\nInflation: ").append(d.inflation)
                          .append("\nUnemployment: ").append(d.unemployment)
                          .append("\n\n");
                    found = true;
                }
            }

            if (found) {
                JOptionPane.showMessageDialog(this, result.toString());
            } else {
                JOptionPane.showMessageDialog(this, "No data found for year: " + year);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid year input!");
        }
    }

    private void saveData(List<EconomicData> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new GZIPOutputStream(new FileOutputStream("economic_data.gz")))) {
            oos.writeObject(list);
            System.out.println("Data saved in compressed format.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<EconomicData> loadData() {
        File file = new File("economic_data.gz");
        if (!file.exists()) return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {
            return (List<EconomicData>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

class EconomicData implements Serializable {
    private static final long serialVersionUID = 1L;

    int year;
    double gdp;
    double inflation;
    double unemployment;

    public EconomicData(int year, double gdp, double inflation, double unemployment) {
        this.year = year;
        this.gdp = gdp;
        this.inflation = inflation;
        this.unemployment = unemployment;
    }
}

class GraphPanel extends JPanel {

    private List<EconomicData> dataList;

    public GraphPanel(List<EconomicData> dataList) {
        this.dataList = new ArrayList<>(dataList);
        setBackground(Color.WHITE);
        addForecast(2);
    }

    public void updateData(List<EconomicData> newData) {
        this.dataList = new ArrayList<>(newData);
        addForecast(2);
        repaint();
    }

    private void addForecast(int yearsAhead) {
        if (dataList.size() < 2) return;

        EconomicData last = dataList.get(dataList.size() - 1);
        EconomicData secondLast = dataList.get(dataList.size() - 2);

        double gdpSlope = last.gdp - secondLast.gdp;
        double inflSlope = last.inflation - secondLast.inflation;
        double unempSlope = last.unemployment - secondLast.unemployment;

        for (int i = 1; i <= yearsAhead; i++) {
            dataList.add(new EconomicData(
                    last.year + i,
                    last.gdp + gdpSlope * i,
                    last.inflation + inflSlope * i,
                    last.unemployment + unempSlope * i
            ));
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        int padding = 60;

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Economic Trends Dashboard", width / 2 - 120, 30);

        g2.drawLine(padding, height - padding, width - padding, height - padding);
        g2.drawLine(padding, padding, padding, height - padding);

        if (dataList.isEmpty()) return;

        double maxVal = dataList.stream()
                .flatMapToDouble(d -> java.util.stream.DoubleStream.of(d.gdp, d.inflation, d.unemployment))
                .max().orElse(10);

        double minVal = dataList.stream()
                .flatMapToDouble(d -> java.util.stream.DoubleStream.of(d.gdp, d.inflation, d.unemployment))
                .min().orElse(0);

        int pointSpacing = (width - 2 * padding) / (dataList.size() - 1);

        java.util.function.DoubleFunction<Integer> yCoord = val ->
                height - padding - (int) ((val - minVal) /
                        (maxVal - minVal == 0 ? 1 : (maxVal - minVal)) *
                        (height - 2 * padding));

        drawLine(g2, dataList, pointSpacing, yCoord, "gdp", Color.BLUE);
        drawLine(g2, dataList, pointSpacing, yCoord, "inflation", Color.RED);
        drawLine(g2, dataList, pointSpacing, yCoord, "unemployment", Color.GREEN);

        g2.setColor(Color.BLACK);
        for (int i = 0; i < dataList.size(); i++) {
            int x = padding + i * pointSpacing;
            g2.drawString(String.valueOf(dataList.get(i).year), x - 15, height - padding + 20);
        }

        g2.setColor(Color.BLUE);
        g2.drawString("GDP", width - 200, 30);
        g2.setColor(Color.RED);
        g2.drawString("Inflation", width - 200, 50);
        g2.setColor(Color.GREEN);
        g2.drawString("Unemployment", width - 200, 70);
    }

    private void drawLine(Graphics2D g2, List<EconomicData> dataList, int pointSpacing,
                          java.util.function.DoubleFunction<Integer> yCoord, String type, Color color) {
        g2.setColor(color);

        for (int i = 0; i < dataList.size() - 1; i++) {
            int x1 = 60 + i * pointSpacing;
            int x2 = 60 + (i + 1) * pointSpacing;

            double val1 = switch (type) {
                case "gdp" -> dataList.get(i).gdp;
                case "inflation" -> dataList.get(i).inflation;
                case "unemployment" -> dataList.get(i).unemployment;
                default -> 0;
            };

            double val2 = switch (type) {
                case "gdp" -> dataList.get(i + 1).gdp;
                case "inflation" -> dataList.get(i + 1).inflation;
                case "unemployment" -> dataList.get(i + 1).unemployment;
                default -> 0;
            };

            g2.drawLine(x1, yCoord.apply(val1), x2, yCoord.apply(val2));
        }
    }
}