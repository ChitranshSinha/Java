import mdmreltioconnect.ReltioConnect;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class general {
  public static String r_username , r_password , r_auth_url , r_application_url , r_basic_auth ,r_tenant_url, r_tenant;
  public static String countriesList,lookUpFilePath;

  // method to read tenant parameter
  public static void  ReadConnectionParameter(String propertiesFilePath) {
    try {
      FileReader propFile = new FileReader(propertiesFilePath);
      Properties prop = new Properties();
      prop.load(propFile);

      r_username= prop.get("USERNAME").toString();
      r_password= prop.get("PASSWORD").toString();
      r_auth_url= prop.get("AUTH_URL").toString();
      r_application_url= prop.getProperty("APPLICATIONURL");
      r_basic_auth = prop.getProperty("ReltioBasicAuth" );
      r_tenant_url = prop.getProperty("ReltioTenantUrl");
      r_tenant = prop.getProperty("TENANT");
      countriesList = prop.getProperty("CountriesList");
      lookUpFilePath = prop.getProperty("LookUpFilePath");
    }
    catch (Exception ex)
    {
      System.out.println("Error - Reading properties file in ReadConnectionParameter: " + ex.toString());
    }
  }

  // method to read CSV file
  public static List<List<String>>  readCSVFile(String filePath){
    List<List<String>> lookUpFileData = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      boolean isFirstLine = true;
      while ((line = br.readLine()) != null) {
        if (isFirstLine) {
          isFirstLine = false;
          continue; // Skip the header row
        }
        String[] values = line.split("\\|");
        if (values.length == 4) lookUpFileData.add(Arrays.asList(values));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lookUpFileData;
  }

  // method to return zip5value (ie ZipCode) from scan call response
  public static String checkAndGetZip5 (JSONObject attributes) throws JSONException {
    if (attributes.has("Zip")) {
      JSONObject zip = attributes.getJSONArray("Zip").optJSONObject(0);
      if (zip.has("value")){
        JSONObject zipValue = zip.getJSONObject("value");
        if (zipValue.has("Zip5")){
          return zipValue.getJSONArray("Zip5").getJSONObject(0).getString("value");
        } else return "";
      } else return "";
    } else return "";
  }

  // method to return matching Brick type and Brick Value from the lookup CSV data (in memory)
  public static String[] getBrickTypeAndValue (String country , String zipCode, List<List<String>> lookUpData) {
    for (int j=0 ; j< lookUpData.size();j++){
      List <String> line = lookUpData.get(j);
      if ( line.get(0).equals(country) && line.get(1).equals(zipCode) ) {
        return new String [] {line.get(2),line.get(3)};
      }
    }
    return null;
  }

  // method to update the correct zip values of the location entities
  public static void updateBrick (String brickUpdateUrl ,String uri , String brickType ,String brickValue,  ReltioConnect m_connRel ) {
    StringBuilder sbError = new StringBuilder("");
    sbError.setLength(0);

    try {
      String brickUpdateBody = "[\n" +
         "	{\n" +
         "		\"type\": \"configuration/entityTypes/Location\",\n" +
         "		\"crosswalks\": [ \n" +
         "			{\n" +
         "				\"type\": \"configuration/sources/Reltio\",\n" +
         "				\"value\": \"" + uri +  "\"\n" +
         "			}\n" +
         "		],\n" +
         "		\"attributes\": {\n" +
         "			\"Brick\": [\n" +
         "				{\n" +
         "					\"value\": {\n" +
         "						\"Type\": [\n" +
         "							{\n" +
         "								\"value\":  \"" + brickType  + "\"\n" +
         "							}\n" +
         "						],\n" +
         "						\"BrickValue\": [\n" +
         "							{\n" +
         "								\"value\":  \"" + brickValue  +"\"\n"  +
         "							}\n" +
         "						]\n" +
         "					}\n" +
         "				}\n" +
         "			]\n" +
         "		}\n" +
         "	}\n" +
         "]\n";
      m_connRel.SendRequest(brickUpdateUrl, "POST", brickUpdateBody, sbError);
      if (sbError.length() == 0)           System.out.println("Success: " + uri + ":" + brickType + ":" + brickValue );
      else                                 System.out.println("Fail: "    + uri + ":" + brickType + ":" + brickValue );
    } catch (Exception ex) {
      System.out.println("Error in Updating Brick value in updateBrick method ."+ uri + ":" + brickType + ":" + brickValue + " :" + ex.toString());
    }
  }

}