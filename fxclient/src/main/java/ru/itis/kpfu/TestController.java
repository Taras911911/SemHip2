package ru.itis.kpfu;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private ObservableList<Product> productsData = FXCollections.observableArrayList();

//    private Parent parent;
//    private Scene scene;
//    private Stage stage;

    @FXML
    private TableView<Product> tableProducts;
    @FXML
    private TableColumn<Product, String> idColumn;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, String> typeColumn;
    @FXML
    private TableColumn<Product, String> descriptionColumn;
    @FXML
    private TableColumn<Product, String> priceColumn;
    @FXML
    private TextField loginText;
    @FXML
    private TextField passwordText;
    @FXML
    private TextField statusText;

    public void signIn() {
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet("http://localhost:8880/api/stocks");
                    byte[] encodedBytes = java.util.Base64.getEncoder().encode((loginText.getText() + ":" + passwordText.getText()).getBytes());
                    String encoding = new String(encodedBytes);
                    httpGet.setHeader("Authorization", "Basic " + encoding);

                    log.debug("Http request sent to  " + httpGet.getURI());
                    CloseableHttpResponse response1 = httpclient.execute(httpGet);
                    if (response1.getStatusLine().getStatusCode()== 200)
                        statusText.setText("Authorization passed");
                    else
                        statusText.setText("Authorization failed");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void saveChanges() {
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    ObservableList<Product> products = tableProducts.getItems();
                    JSONArray tableDataJSON = new JSONArray();

                    for (Product product : products) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", product.getId());
                        jsonObject.put("name", product.getName());
                        jsonObject.put("type", product.getType());
                        jsonObject.put("description", product.getDescription());
                        jsonObject.put("price", product.getPrice());
                        tableDataJSON.put(jsonObject);
                    }

                    //Создание http запроса
                    HttpURLConnection urlConnection = null;
                    BufferedOutputStream bos = null;
                    URL url = new URL("http://localhost:8880/api/update");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestMethod("POST");
                    byte[] encodedBytes = java.util.Base64.getEncoder().encode((loginText.getText() + ":" + passwordText.getText()).getBytes());
                    String encoding = new String(encodedBytes);
                    urlConnection.setRequestProperty("Authorization", "Basic "+ encoding);
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.connect();

                    bos = new BufferedOutputStream(urlConnection.getOutputStream());
                    bos.write(tableDataJSON.toString().getBytes());

                    bos.flush();
                    bos.close();

                    String result = urlConnection.getResponseMessage();
                    if(result.equals("OK"))
                        statusText.setText("Changes saved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void getItems() {
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    tableProducts.getItems().clear();
                    tableProducts.setEditable(true);
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet("http://localhost:8880/api/stocks");

                    //basic auth
                    byte[] encodedBytes = java.util.Base64.getEncoder().encode((loginText.getText() + ":" + passwordText.getText()).getBytes());
                    String encoding = new String(encodedBytes);
                    httpGet.setHeader("Authorization", "Basic " + encoding);


                    log.debug("Http request sent to  " + httpGet.getURI());
                    CloseableHttpResponse response1 = httpclient.execute(httpGet);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response1.getEntity().getContent()));
                    StringBuffer result = new StringBuffer();
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }

                    JSONArray o = new JSONArray(result.toString());
                    idColumn.setCellValueFactory(new PropertyValueFactory<Product, String>("id"));
                    typeColumn.setCellValueFactory(new PropertyValueFactory<Product, String>("type"));
                    nameColumn.setCellValueFactory(new PropertyValueFactory<Product, String>("name"));
                    descriptionColumn.setCellValueFactory(new PropertyValueFactory<Product, String>("description"));
                    priceColumn.setCellValueFactory(new PropertyValueFactory<Product, String>("price"));

                    //Редактирование ячеек
                    //name
                    nameColumn.setCellFactory(TextFieldTableCell.<Product> forTableColumn());
                    nameColumn.setOnEditCommit((CellEditEvent<Product, String> event) -> {
                        TablePosition<Product, String> pos = event.getTablePosition();

                        String newName = event.getNewValue();

                        int row = pos.getRow();
                        Product product = event.getTableView().getItems().get(row);

                        product.setName(newName);
                    });

                    //id
                    idColumn.setCellFactory(TextFieldTableCell.<Product> forTableColumn());

                    //type
                    typeColumn.setCellFactory(TextFieldTableCell.<Product> forTableColumn());
                    typeColumn.setOnEditCommit((CellEditEvent<Product, String> event) -> {
                        TablePosition<Product, String> pos = event.getTablePosition();
                        String newType = event.getNewValue();
                        int row = pos.getRow();
                        Product product = event.getTableView().getItems().get(row);
                        product.setType(newType);
                    });

                    //description
                    descriptionColumn.setCellFactory(TextFieldTableCell.<Product> forTableColumn());
                    descriptionColumn.setOnEditCommit((CellEditEvent<Product, String> event) -> {
                        TablePosition<Product, String> pos = event.getTablePosition();
                        String newDesc = event.getNewValue();
                        int row = pos.getRow();
                        Product product = event.getTableView().getItems().get(row);
                        product.setDescription(newDesc);
                    });

                    //price
                    priceColumn.setCellFactory(TextFieldTableCell.<Product> forTableColumn());
                    priceColumn.setOnEditCommit((CellEditEvent<Product, String> event) -> {
                        TablePosition<Product, String> pos = event.getTablePosition();
                        String newPrice = event.getNewValue();
                        int row = pos.getRow();
                        Product product = event.getTableView().getItems().get(row);
                        product.setPrice(newPrice);
                    });

                    tableProducts.setItems(productsData);
                    for (int i = 0; i < o.length(); i++)
                    {
                        productsData.addAll(new Product(
                                o.getJSONObject(i).getString("name"),
                                String.valueOf(o.getJSONObject(i).getInt("id")),
                                o.getJSONObject(i).getString("type"),
                                o.getJSONObject(i).getString("description"),
                                String.valueOf(o.getJSONObject(i).getDouble("price"))));
                    }
                    if(response1.getStatusLine().getStatusCode()== 200)
                        statusText.setText( "\n Loading data");
                    else statusText.setText("Access denied");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
