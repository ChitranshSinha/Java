
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import mdmreltioconnect.ReltioConnect;

import com.google.gson.Gson;
import java.util.*;

public class general {
  public static String r_username , r_password , r_auth_url , r_application_url , r_basic_auth ,r_tenant_url, r_tenant;
  public static String scan_filter,scan_select, scan_options;

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
      scan_filter = prop.getProperty("SCAN_FILTER");
      scan_select = prop.getProperty("SCAN_SELECT_QUERY");
      scan_options = prop.getProperty("SCAN_OPTIONS_QUERY");
    } catch (Exception ex)
    {
      System.out.println("Error - Reading properties file in ReadConnectionParameter: " + ex.toString());
    }
  }

  // method to get access token
  public static String getAccessToken(String AuthUrl , String username , String password) throws IOException, JSONException {
    String Auth_url= AuthUrl + "/token?username="+username +"&password="+password + "&grant_type=password";
    // Create the HTTPS connection
    URL obj = new URL(Auth_url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("POST");
    con.setRequestProperty("Authorization", "Basic cmVsdGlvX3VpOm1ha2l0YQ==");

    // Get the response and extract the access token from it
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder response = new StringBuilder();
    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    JSONObject jsonResponse = new JSONObject(response.toString());
    return jsonResponse.getString("access_token");
  }

  // method to call Reltio using url
  public static StringBuilder ReltioCallFull (String reltioUrl , String accessToken) throws IOException {
    StringBuilder response = null;
    try {
      URL obj = new URL(reltioUrl);
      String Auth = "Bearer " + accessToken;
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/json");
      con.setRequestProperty("Authorization", Auth);
      con.setDoOutput(true);
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      response = new StringBuilder();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine); }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (response);
  }

  // method (recursive) to find and update the level of child and so on..
  public static void findChildUpdateLevel(String entity , String reltio_tenant_url , ReltioConnect m_connRel ,  int level ){
    String childEntities = general.findChildFromEntityId(entity,reltio_tenant_url, m_connRel , level);
    int tier=level;
    if (!childEntities.equals("FAIL") && level < 11 ) {
      level = tier + 1 ;
      for (String childEntity: childEntities.split(",") ) {
        findChildUpdateLevel(childEntity,reltio_tenant_url,m_connRel, level );
      }
    }
  }

  // method to find and return child entityIDs URI
  public static String findChildFromEntityId (String currEntity, String reltio_tenant_url , ReltioConnect m_connRel , int level) {
    StringBuilder sbError = new StringBuilder("");
    String hops_url = reltio_tenant_url + "/entities/" + currEntity+  "/_hops?options=ovOnly,resolveMergedEntities&relationTypeURIs=GIndicationGIndication&deep=1&select=uri,label,entities.attributes.Level,entities.attributes.IsCharacteristic";
    try {
      String hopsCallResponse = m_connRel.SendRequest(hops_url, "GET", null, sbError);
      if (sbError.length() == 0) {
        String childEntity = findChild(currEntity, hopsCallResponse);
        if (childEntity.equals("")) { // no Child Found
          return ("FAIL");
        } else {
          String validChild = "";
          for (String currChildEntity : childEntity.split(",")) {

            JSONObject jsonObj = new JSONObject(hopsCallResponse);
            JSONArray entitiesArrayBody = jsonObj.getJSONArray("entities");

            for (int k = 0; k < entitiesArrayBody.length(); k++) {

              JSONObject entity = entitiesArrayBody.getJSONObject(k);
              if (entity.has("uri")) {
                if (entity.getString("uri").split("/")[1].equals(currChildEntity)) {
                  if (entity.has("attributes") && entity.getJSONObject("attributes").has("IsCharacteristic")) {
                    if (entity.getJSONObject("attributes").getJSONArray("IsCharacteristic").getJSONObject(0).getString("value").equalsIgnoreCase("true")) {
                      if (checkIfUpdateLevelNeeded(currChildEntity, 0, entity))
                        updateLevel(reltio_tenant_url + "/entities?options=partialOverride,updateAttributeUpdateDates", currChildEntity, 0, m_connRel);
                    }
                    else {
                      if (checkIfUpdateLevelNeeded(currChildEntity,level +1, entity  )) {
                        updateLevel(reltio_tenant_url + "/entities?options=partialOverride,updateAttributeUpdateDates", currChildEntity, level + 1, m_connRel);
                      }
                      validChild = validChild + currChildEntity + ",";
                    }
                  }
                }
              } else {
                System.out.println("uri key missing for entity " + currChildEntity + "for level " + level + 1);
                updateLevel(reltio_tenant_url + "/entities?options=partialOverride,updateAttributeUpdateDates", currChildEntity, level + 1, m_connRel);
              }
            }
          }
          return validChild;
        }
      } else return ("FAIL");
    }
    catch (Exception ex) {
      System.out.println("Error in findChildFromEntityId method for " + currEntity + " .\n Error : " + ex.toString());
    }
    return ("FAIL") ;
  }

  // method to find child based on match criteria
  private static String findChild ( String currEntity, String hopsResponseData ) {
    try {
      JSONObject jsonObjBody   = new JSONObject(hopsResponseData);
      if (jsonObjBody.has("relations")) {
        JSONArray jsonArrayBody = jsonObjBody.getJSONArray("relations");
        String childObjects="";
        for (int j = 0; j < jsonArrayBody.length(); j++) {
          JSONObject jRelation = (JSONObject) jsonArrayBody.get(j);
          JSONObject endObject = jRelation.getJSONObject("endObject");
          String parentEndObjectURI = endObject.getString("objectURI");
          if (("entities/" + currEntity).equals(parentEndObjectURI)) {
            if (jRelation.getJSONObject("startObject").has("objectURI")) {
              String childStartObject = jRelation.getJSONObject("startObject").getString("objectURI");
              childObjects = childObjects + childStartObject.split("/")[1] + "," ;
            }
          }
        }
        return childObjects;
      }
      else return ("");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean checkIfUpdateLevelNeeded (String entityID, int level , JSONObject entity) throws JSONException {
    boolean updateLevelNeeded= true;
    String tier= null;
    if (level > 0 ) tier= String.valueOf(level) ;

    if (entity.getJSONObject("attributes").has("Level")) {
     if  (entity.getJSONObject("attributes").getJSONArray("Level").getJSONObject(0).getString("value").equalsIgnoreCase(tier)){
       return false;
      }
    }
    else if (!entity.getJSONObject("attributes").has("Level") && level==0){
      return false;
    }
    return  updateLevelNeeded;
  }

  public static void updateLevel ( String levelUpdateUrl ,String entityId , int level ,  ReltioConnect m_connRel ) {
    StringBuilder sbError = new StringBuilder("");
    sbError.setLength(0);
    String tier= null;
    if (level > 0 ) tier= String.valueOf(level) ;

    try {
      String levelUpdateBody = "[\n" +
         "	{\n" +
         "     \"attributes\" : { \n" +
         "			\"Level\" : [\n" +
         "							 {\n " +
         "									\"value\": "+ tier + "\n" +
         "							}\n" +
         "						]\n" +
         "						},\n" +
         "	                \"type\": \"configuration/entityTypes/GlobalIndication\",\n" +
         "                  \"crosswalks\": [\n" +
         "						{\n" +
         "							\"type\" : \"configuration/sources/Reltio\",\n" +
         "							\"value\": \"" + entityId + "\"\n" +
         "						}\n" +
         "						]\n" +
         " }\n" +
         "]"         ;
      m_connRel.SendRequest(levelUpdateUrl, "POST", levelUpdateBody, sbError);
      if (sbError.length() == 0)           System.out.println("Success: " + entityId +" = " + tier );
      else                                 System.out.println("Fail: "    + entityId +" = " + tier );
    } catch (Exception ex) {
      System.out.println("Error in Updating Level in updateLevel method for " + entityId + " and level=" + tier  +".Error is :\n"+ ex.toString());
    }
  }

}