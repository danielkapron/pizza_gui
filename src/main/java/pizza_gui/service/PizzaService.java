package pizza_gui.service;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import pizza_gui.model.Ingredient;
import pizza_gui.model.Pizza;
import pizza_gui.model.PizzaModel;

import javax.xml.soap.Text;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PizzaService {

    // metoda wprowadzająca dane do ObservableList
    public ObservableList<PizzaModel> addPizzas(ObservableList<PizzaModel> pizzas){
        // type -> WEGE lub SPICY
        for (Pizza pizza : Pizza.values()){
            pizzas.add(new PizzaModel(
                    pizza.getName(),
                    pizza.getIngredients().stream().map(Ingredient::getName).collect(Collectors.joining(",")),
                    (pizza.getIngredients().stream().anyMatch(Ingredient::isSpicy) ? "ostra " : "")
                            +
                            (pizza.getIngredients().stream().noneMatch(Ingredient::isMeat) ? "wege " : ""),
                    pizza.getIngredients().stream().mapToDouble(Ingredient::getPrice).sum()
            ));
        }
        return pizzas;
    }


    public void insertPizzasToTable(
            TableView<PizzaModel> tblPizza,
            TableColumn<PizzaModel, String> tcName,
            TableColumn<PizzaModel, String> tcIngredients,
            TableColumn<PizzaModel, String> tcType,
            TableColumn<PizzaModel, Double> tcPrice,
            ObservableList<PizzaModel> pizzas
    ){
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcIngredients.setCellValueFactory(new PropertyValueFactory<>("ingredients"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        // ustawienie języka i formatowanie wartości double
        Locale locale = new Locale("pl", "PL");
        // obiekt do wartości numerycznych
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
        tcPrice.setCellFactory(tc -> new TableCell<PizzaModel,Double>(){
            @Override
            protected void updateItem(Double price, boolean empty){
                super.updateItem(price, empty);
                if(empty){
                    setText(null);
                } else {
                    setText(currencyFormat.format(price));
                }
            }
        });
        tblPizza.setItems(pizzas);
    }

    //generator pizzy dnia -> 1. obniżenie ceny wylosowanej pizzy, 2. przekazanie nazwy pizzy dnia do Label'a

    public void pizzaOfTheDayGenerator(ObservableList<PizzaModel> pizzas, Label randomPizza){
        //losowanie pizzy
        int randomIndex = new Random().nextInt(pizzas.size());
        PizzaModel pizzaOfTheDay = pizzas.get(randomIndex);
        // wypisanie nazwy w Label'u
       randomPizza.setText(pizzaOfTheDay.getName());
       //obniżenie ceny
        pizzas.get(randomIndex).setPrice(pizzas.get(randomIndex).getPrice() * 0.8);
        // wypisanie nazwy pizzy przy Labelu
        randomPizza.setText(String.format("%s - %.2f zł", pizzaOfTheDay.getName(),pizzaOfTheDay.getPrice()));
    }

    private List<Integer> choices = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10));
    // obiekt przechowujacy zapłate
    private double amount;
    // metoda do przenoszenia pizzy do koszyka
    public void addToBasket(TableView<PizzaModel> tblPizza, TextArea taBasket, Label lblSum){
        // odczyt ktory wiersz w tabelce zostal oznaczony
        PizzaModel selectedPizza = tblPizza.getSelectionModel().getSelectedItem();
        // utworzenie okna kontekstowego do zamówienia wybranej ilości pizzy
        ChoiceDialog<Integer> addToBasketDialog = new ChoiceDialog<>(1, choices);
        addToBasketDialog.setTitle("Wybierz ilość");
        addToBasketDialog.setHeaderText("Wybrałeś pizzę: " + selectedPizza.getName());
        addToBasketDialog.setContentText("Wybierz ilość zamawianej pizzy: ");
        // okno zostaje wyświetlone i utrzymane na ekranie i zwróci wartość po wciśnięciu przycisku
        Optional<Integer> result = addToBasketDialog.showAndWait();
        // gdy wybrano OK
        result.ifPresent(quantity -> taBasket.appendText(
                String.format("%-15s %5d szt. %10.2f zł\n",
                        selectedPizza.getName(),
                        quantity,
                        selectedPizza.getPrice() * quantity)
                ));
        //gdy wybrano OK na naszym przycisku
        result.ifPresent(quantity -> amount = amount + (quantity * selectedPizza.getPrice()));
        lblSum.setText(String.format("KWOTA DO ZAPŁATY: %.2f ZŁ", amount));

    }


    public void clearOrder(TextArea taBasket, TextField tfAddress, TextField tfPhone, Label lblSum){
        taBasket.clear();
        tfAddress.clear();
        tfPhone.clear();
        amount = 0;
        lblSum.setText("KWOTA DO ZAPŁATY: 0.00 ZŁ");
    }

    public boolean isPhoneValid(String phone){
        return Pattern.matches("(^([0-9]{3}[-]{1}){2}[0-9]{3}$)|(^[0-9]{9}$)", phone);
    }

    public boolean isAddressValid(String address){
        return Pattern.matches("^[au][l][\\.]\\s{0,1}[A-Za-złąęśćźżóń\\d\\.\\s]{1,}\\s{1}\\d{1,}[A-Za-z]{0,}[\\/]{0,1}\\d{0,}[,]\\s{0,1}\\d{2}[-]\\d{3}\\s{1}[A-Za-złąęśćźżóń\\s\\-]{2,}$", address);
    }

    //okno dialogowe typu information albo error potweirdzajace lub odrzcuajace
    public void getOrder(TextField tfPhone, TextField tfAddress, TextArea taBasket, Label lblSum){
        if(isPhoneValid(tfPhone.getText()) && isAddressValid(tfAddress.getText()) && !taBasket.getText().equals("")){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Zamówienie");
        alert.setHeaderText("Potwierdznie zamówienia");
        alert.setContentText("Twoje zamówienie: \n" + taBasket.getText() + "\nDo zapłaty: " + amount + " zł");
        alert.showAndWait();
        clearOrder(taBasket, tfAddress, tfPhone, lblSum);
        } else {
        Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Zamówienie");
            alert.setHeaderText("Błędne dane zamówienia");
            String validationResult = "Wprowadziłeś niepoprawne dane w następujących polach: ";
            if(!isPhoneValid(tfPhone.getText())){
                validationResult += "Błędny numer telefonu ";
            }
            if(!isPhoneValid(tfPhone.getText()) && isAddressValid(tfAddress.getText())){
                validationResult += "";
            }
            if(isAddressValid(tfAddress.getText())){
                validationResult += "Błędny adres dostawy ";
            }
            String emptyBasket = "";
            if(taBasket.getText().equals("")){
                emptyBasket = "\nTwój koszyk nie może być pusty.";
            }
            alert.setContentText(validationResult + emptyBasket);
            alert.showAndWait();
        }
    }
        public void saveDataFile(TextField tfAddress, TextField tfPhone, TextArea taBaket) throws FileNotFoundException {
        // data i czas zamowienia
            // adres dostawy
            // telefon
            // czas dostawy to data i czas zamoweinai + 45min
            // ------------------------------------------------
            //f zawartosc koszyka
            // kwota do zaplaty

            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter exFilter = new FileChooser.ExtensionFilter("plik tekstowy (*.txt", "*.txt");
            fileChooser.getExtensionFilters().add(exFilter);

            File file = fileChooser.showSaveDialog(null);

            PrintWriter printWriter = new PrintWriter(file);
            printWriter.println("POTWIERDZENIE ZAMÓIENIA");
            LocalDateTime dateTime = LocalDateTime.now();
            printWriter.println("Data i czas zamówienia" + dateTime);
            printWriter.println("Adres dostawy: " + tfAddress.getText());
            printWriter.println("Telefon kontaktowy: " + tfPhone.getText());
            printWriter.println("Czas dostawy" + dateTime.plusMinutes(45));
            printWriter.println("Zamówione produkty: \n" + taBaket.getText());
            printWriter.println("Suma do zapłąty: " + amount +  " zł");
            printWriter.close();
        }


}
