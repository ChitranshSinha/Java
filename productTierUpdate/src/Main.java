
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import mdmreltioconnect.ReltioConnect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {
  public static String reltio_username , reltio_password , reltio_auth_url , reltio_application_url , reltio_basic_auth,reltio_tenant;
  public static String reltio_tenant_url,scan_filter,scan_select, scan_options;
  static String reltioScanUrlFull ;
  static StringBuilder reltioScanCallResponse;
  static ReltioConnect m_connRel = null;
  IsCharacteristicObject iCObject = new IsCharacteristicObject();

  public static void main(String args[]) throws JSONException, IOException {
   // String propertiesFilePath = "C:\\Users\\csinha\\IdeaProjects\\gIndicationLevel\\out\\production\\gIndicationLevel\\tenant.properties";
    String propertiesFilePath = args[0];

    try {
      general.ReadConnectionParameter(propertiesFilePath);
      reltio_username = general.r_username;
      reltio_password = general.r_password;
      reltio_auth_url = general.r_auth_url;
      reltio_application_url = general.r_application_url;
      reltio_tenant = general.r_tenant;
      reltio_tenant_url = general.r_tenant_url;
      reltio_basic_auth = general.r_basic_auth;
      scan_select = general.scan_select;
      scan_filter = general.scan_filter;
      scan_options = general.scan_options;
    } catch (Exception ex) {
      System.out.println("Error in reading properties file in main method : " + ex.toString());
    }

    if (reltio_username != null && reltio_password != null) {

      reltioScanUrlFull = reltio_tenant_url + reltio_tenant + "/entities/_scan?" + scan_filter + "&" + scan_select + "&" + scan_options;
      reltioScanCallResponse = general.ReltioCallFull(reltioScanUrlFull, general.getAccessToken(reltio_auth_url, reltio_username, reltio_password));

      m_connRel = new ReltioConnect(reltio_username, reltio_password, reltio_basic_auth, reltio_auth_url, reltio_tenant);
      IsCharacteristicObject iCObject = generateIsCharacteristicList(reltioScanCallResponse);

      for (String currEntity: iCObject.TrueListUpdateNeeded) {
        general.updateLevel(reltio_tenant_url + reltio_tenant + "/entities?options=partialOverride,updateAttributeUpdateDates", currEntity, 0, m_connRel);
      }

      for (String currEntity : iCObject.FalseListUpdateNeeded) {
        general.updateLevel(reltio_tenant_url + reltio_tenant + "/entities?options=partialOverride,updateAttributeUpdateDates", currEntity, 1, m_connRel);
      }

      /*
      for (String currEntity : iCObject.getIsCharacteristicTrueList()) {
        general.updateLevel(reltio_tenant_url + reltio_tenant + "/entities?options=partialOverride,updateAttributeUpdateDates", currEntity, 0, m_connRel);
      }  not needed */

      for (String currEntity : iCObject.getIsCharacteristicFalseList()) {
        // String currEntity="1dT429ZA"; // 1dT3waKG" , for testing in B8
        // currEntity= "2Aj2AyrJ";
       // general.updateLevel(reltio_tenant_url + reltio_tenant + "/entities?options=partialOverride,updateAttributeUpdateDates", currEntity, 1, m_connRel);
       general.findChildUpdateLevel(currEntity, reltio_tenant_url + reltio_tenant, m_connRel, 1);
      }
    } else System.out.println("username/password is not provided");
  }

    // method to generate isCharacteristic True/false list in Object
    private static IsCharacteristicObject generateIsCharacteristicList (StringBuilder scanData) throws JSONException{
      IsCharacteristicObject iCObject = new IsCharacteristicObject();
      List<String> isCharacteristicTrueList   = iCObject.getIsCharacteristicTrueList();
      List<String> isCharacteristicFalseList  = iCObject.getIsCharacteristicFalseList();

      List<String> trueListUpdate= iCObject.TrueListUpdateNeeded;
      List<String> falseListUpdate = iCObject.FalseListUpdateNeeded;

      JSONObject jsonObj = new JSONObject(scanData.toString());
      JSONArray jsonArray = jsonObj.getJSONArray("objects");

      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject obj = jsonArray.getJSONObject(i);
        JSONObject attributes = obj.getJSONObject("attributes");
        if (attributes.has("IsCharacteristic")) {
          JSONArray isCharacteristicArray = attributes.getJSONArray("IsCharacteristic");
          String isCharacteristicValue = isCharacteristicArray.getJSONObject(0).getString("value");
          if (isCharacteristicValue.equalsIgnoreCase("true")) {
            String isCharTrueEntityId = obj.getString("uri").split("/")[1];
            if (checkIfUpdateLevelNeededScanBody(isCharTrueEntityId,0,attributes))  trueListUpdate.add(isCharTrueEntityId);
            isCharacteristicTrueList.add(isCharTrueEntityId);
          }
          else if (isCharacteristicValue.equalsIgnoreCase("false")) {
            String isCharFalseEntityId = obj.getString("uri").split("/")[1];
            if (checkIfUpdateLevelNeededScanBody(isCharFalseEntityId,1,attributes)) falseListUpdate.add(isCharFalseEntityId);
            isCharacteristicFalseList.add(isCharFalseEntityId);
          }
        }
      }
      return iCObject;
    }

    // method to check if level are matching. from the Scan Call Response
  private static boolean checkIfUpdateLevelNeededScanBody (String entityID, int level , JSONObject attributes) throws JSONException {
    boolean updateLevelNeeded= true;
    String tier= null;
    if ((level > 0) && attributes.has("Level")){
      tier= String.valueOf(level) ;
      if  (attributes.getJSONArray("Level").getJSONObject(0).getString("value").equalsIgnoreCase(tier))  return false ;
      else return true;
    }
    else if ((level > 0) && (!attributes.has("Level"))) return true;
    else if ((level == 0) && attributes.has("Level"))   return true;
    else if ((level == 0) && (!attributes.has("Level")))return false ;
    return  updateLevelNeeded;
  }
}
