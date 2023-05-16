import mdmreltioconnect.ReltioConnect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BrickUpdate {
  public static String reltio_username, reltio_password, reltio_auth_url, reltio_application_url, reltio_basic_auth, reltio_tenant;
  public static String reltio_tenant_url, countries;
  static String reltioScanUrlFull;
  static ReltioConnect m_connRel = null;

  public static void main(String[] args) throws JSONException, IOException {
    String propertiesFilePath = args[0];
    String filePath = args[1];
    try {
      general.ReadConnectionParameter(propertiesFilePath);
      reltio_username = general.r_username;
      reltio_password = general.r_password;
      reltio_auth_url = general.r_auth_url;
      reltio_application_url = general.r_application_url;
      reltio_tenant = general.r_tenant;
      reltio_tenant_url = general.r_tenant_url;
      reltio_basic_auth = general.r_basic_auth;
      countries = general.countriesList;
    } catch (Exception ex) {
      System.out.println("Error in reading properties file in main method : " + ex.toString());
    }

    m_connRel = new ReltioConnect(reltio_username, reltio_password, reltio_basic_auth, reltio_auth_url, reltio_tenant);
       List<List<String>> lookUpFileData =  general.readCSVFile(filePath);

    for (String country : countries.split(",")) {
      reltioScanUrlFull = reltio_tenant_url + reltio_tenant + "/entities/_scan?filter=(equals(type,'configuration/entityTypes/Location')and%20equals(attributes.Country,'" + country + "')and%20exists(attributes.Zip)%20and%20missing(attributes.Brick))&max=50&select=uri,attributes.Zip";

      List<ObjectLocationDetails> objectList = new ArrayList<ObjectLocationDetails>();
      getAllLocationAndZip5 (reltioScanUrlFull,m_connRel,objectList,country,"" );

      for (ObjectLocationDetails oneDetail: objectList) {
        String[] zipData = general.getBrickTypeAndValue(country,oneDetail.zipCode, lookUpFileData);
        if (!(zipData == null)) {
          general.updateBrick(reltio_tenant_url + reltio_tenant + "/entities?options=partialOverride,updateAttributeUpdateDates", oneDetail.entityId, zipData[0], zipData[1], m_connRel);
        }
        else System.out.println("Brick Type and/or Brick Value not present in lookUp file for: " + oneDetail.entityId + " having zipCode " +  oneDetail.zipCode  +  " and country " + country);
      }
    }
  }

  private static void getAllLocationAndZip5(String reltioScanUrlFull, ReltioConnect m_connRel, List<ObjectLocationDetails> objectList, String country, String body) throws JSONException {
    StringBuilder sbError = new StringBuilder();
    sbError.setLength(0);
    try {
      String scanCallResponse = m_connRel.SendRequest(reltioScanUrlFull, "POST", body, sbError);
      JSONObject scanCallResponseObject = new JSONObject(scanCallResponse);
      if (scanCallResponseObject.has("objects")) {
        JSONArray objects = scanCallResponseObject.getJSONArray("objects");
        for (int i = 0; i < objects.length(); i++) {
          JSONObject object = objects.getJSONObject(i);
          ObjectLocationDetails objectLocationDetails = new ObjectLocationDetails();
          JSONObject attributes = object.getJSONObject("attributes");
          String zip5Value = general.checkAndGetZip5(attributes);
          String locEntity = object.getString("uri").split("/")[1];
          if (zip5Value.equals(""))
            System.out.println("zip5 value missing for Location Entity " + locEntity + " for country " + country);
          else {
            objectLocationDetails.setEntityId(locEntity);
            objectLocationDetails.setZipCode(zip5Value);
            objectList.add(objectLocationDetails);
          }
        }

       if (objects.length() >= 50) {
         body = "{\n " +
           " \"cursor\":" + scanCallResponseObject.getJSONObject("cursor").toString() + "\n" +
           "}";
         getAllLocationAndZip5(reltioScanUrlFull, m_connRel, objectList, country, body);
       }
      }
    } catch (Exception ex) {
      System.out.println("Error in getAllLocationAndZip5 method that fetch all the location which have missing brick  : " + ex.toString());
    }
  }

}