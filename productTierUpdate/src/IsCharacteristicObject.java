import java.util.ArrayList;
import java.util.List;

public class IsCharacteristicObject {
  public List<String> isCharacteristicTrueList  ;
  public List<String> isCharacteristicFalseList ;
  public List<String> FalseListUpdateNeeded;
  public List<String> TrueListUpdateNeeded;

  public IsCharacteristicObject(){
    isCharacteristicTrueList  =new ArrayList<>();
    isCharacteristicFalseList =new ArrayList<>();
    FalseListUpdateNeeded     =new ArrayList<>();
    TrueListUpdateNeeded      =new ArrayList<>();

  }

  public List<String> getIsCharacteristicTrueList() {
    return isCharacteristicTrueList;
  }

  public void setIsCharacteristicTrueList(List<String> isCharacteristicTrueList) {
    this.isCharacteristicTrueList = isCharacteristicTrueList;
  }

  public List<String> getIsCharacteristicFalseList() {
    return isCharacteristicFalseList;
  }

  public void setIsCharacteristicFalseList(List<String> isCharacteristicFalseList) {
    this.isCharacteristicFalseList = isCharacteristicFalseList;
  }


  public List<String> getFalseListUpdateNeeded() {
    return FalseListUpdateNeeded;
  }

  public void setFalseListUpdateNeeded(List<String> falseListUpdateNeeded) {
    FalseListUpdateNeeded = falseListUpdateNeeded;
  }

  public List<String> getTrueListUpdateNeeded() {
    return TrueListUpdateNeeded;
  }

  public void setTrueListUpdateNeeded(List<String> trueListUpdateNeeded) {
    TrueListUpdateNeeded = trueListUpdateNeeded;
  }
}

