package ttoview.ubigate.com.nakayosi.test_nfc.model;

import org.json.JSONObject;

/**
 * Created by sungs on 2016-12-15.
 */

public class HttpRequestResult {

    private int resultCode;
    private JSONObject resultJson;

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public JSONObject getResultJson() {
        return resultJson;
    }

    public void setResultJson(JSONObject resultJson) {
        this.resultJson = resultJson;
    }
}
