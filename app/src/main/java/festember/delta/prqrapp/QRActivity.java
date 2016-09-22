package festember.delta.prqrapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class QRActivity extends Activity
	implements com.dlazaro66.qrcodereaderview.QRCodeReaderView.OnQRCodeReadListener {

	private QRCodeReaderView mQRCodeReaderView;
	private SharedPreferences mSharedPreferences;
	private TextView mQrMessageView;
	private Button mButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr_placeholder);
		Log.d("PRQR", "Started");
		int permissionCheck = ContextCompat.checkSelfPermission(this,
			Manifest.permission.CAMERA);
		if(permissionCheck== PermissionChecker.PERMISSION_DENIED) {
			Toast.makeText(this, "Please grant camera permission", Toast.LENGTH_LONG).show();
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, QRUtils.QR_CAMERA_PERMISSION);
		}
		else {
			setContentView(R.layout.activity_qr);
			mButton = (Button) findViewById(R.id.button_scan);
			mQrMessageView = (TextView) findViewById(R.id.textview_qr_instructions);
			mSharedPreferences = getSharedPreferences("AdminDetails",
				Context.MODE_PRIVATE);
			if(!mSharedPreferences.getString("admin_id","").equals("") &&
				 !mSharedPreferences.getString("admin_token","").equals(""))
				mQrMessageView.setText("Scan Client QR code");
			mQRCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrview);
			mQRCodeReaderView.setOnQRCodeReadListener(this);
			mQRCodeReaderView.setBackCamera();
			mQRCodeReaderView.setAlpha(0f);
			mButton.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
					mQRCodeReaderView.setQRDecodingEnabled(true);
					mQRCodeReaderView.setAlpha(1f);
				}
			});
		}

		permissionCheck = ContextCompat.checkSelfPermission(this,
			Manifest.permission.INTERNET);
		if(permissionCheck== PermissionChecker.PERMISSION_DENIED) {
			Toast.makeText(this, "Please grant Internet permission", Toast.LENGTH_LONG).show();
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, QRUtils.QR_INTERNET_PERMISSION);
		}

	}

	@Override
	public void onQRCodeRead(String text, PointF[] points) {
		//admin_id, admin_token, user_id, user_hash
		String admin_id = mSharedPreferences.getString("admin_id", "");
		String admin_token = mSharedPreferences.getString("admin_token", "");
		if(admin_id.equals("") && admin_token.equals("")) {
			try {
				SharedPreferences.Editor sharedPrefEditor = mSharedPreferences.edit();
				Log.d("PRQR", text);

				String[] texts = text.split("\\$\\$\\$");
				//Toast.makeText(QRActivity.this, texts[0]+" - "+texts[1], Toast.LENGTH_SHORT).show();
				if(texts[0]!=null && texts[1]!=null) {
					sharedPrefEditor.putString("admin_id", texts[0]);
					sharedPrefEditor.putString("admin_token", texts[1]);
					sharedPrefEditor.apply();
					mQrMessageView.setText("Admin Successfully scanned");
					//Toast.makeText(QRActivity.this, "Admin successfully scanned", Toast.LENGTH_SHORT).show();
					finish();
				}
			} catch (Exception e) {
				Log.e("PRQR", "Scanning admin"+e.toString());
				Toast.makeText(QRActivity.this, "Error occurred in scanning admin details", Toast.LENGTH_SHORT).show();
			}
		} else {
			mQrMessageView.setText("Scan Client QR code");
			try {
				String[] texts = text.split("\\$\\$\\$");
				//Toast.makeText(QRActivity.this, texts[0]+" - "+texts[1], Toast.LENGTH_SHORT).show();
				if(texts[0]!=null && texts[1]!=null) {
					AsyncSendQRTask asyncSendQRTask = new AsyncSendQRTask();
					asyncSendQRTask.execute(texts[0], texts[1]);
				}
			} catch (Exception e) {
				Toast.makeText(QRActivity.this, "Error occurred in scanning client details", Toast.LENGTH_SHORT).show();
			}
		}
		mQRCodeReaderView.setQRDecodingEnabled(false);
		mQRCodeReaderView.setAlpha(0f);
	}

	public class AsyncSendQRTask extends AsyncTask<String,Void,String> {

		private boolean isSuccess = true;
		private String mResult = null;
		private String mMessage = null;
		@Override
		protected String doInBackground(String... strings) {
			String user_id = strings[0];
			String user_hash = strings[1];
			URL url = null;
			try {
				url = new URL(QRUtils.qrUrl);
			} catch (MalformedURLException e) {
				isSuccess = false;
				Log.d("PRQR", e.toString());
				return null;
			}
			StringBuilder bodyBuilder = new StringBuilder();
			Map<String, String> params = new HashMap<>();
			SharedPreferences sharedPreferences = getSharedPreferences("AdminDetails",
				Context.MODE_PRIVATE);
			String admin_id = sharedPreferences.getString("admin_id","");
			String admin_token = sharedPreferences.getString("admin_token", "");
			if(admin_id.equals(user_id) && admin_token.equals(user_hash)) {
				isSuccess = false;
				return "Same";
			}
			params.put("admin_id", admin_id);
			params.put("admin_token", admin_token);
			params.put("user_id", user_id);
			params.put("user_hash", user_hash);
			Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, String> param = iterator.next();
				bodyBuilder.append(param.getKey()).append('=')
					.append(param.getValue());
				if (iterator.hasNext()) {
					bodyBuilder.append('&');
				}
			}
			String body = bodyBuilder.toString();
			Log.v("Safr", "Posting '" + body + "' to " + url);
			byte[] bytes = body.getBytes();
			HttpURLConnection conn = null;
			try {
				Log.d("URL", "> " + url);
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setFixedLengthStreamingMode(bytes.length);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
				OutputStream out = conn.getOutputStream();
				out.write(bytes);
				out.close();
				InputStream in = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				int responseCode = conn.getResponseCode();
				String line = null;
				String status_code = null;
				// { status_code:200 message:""
				try {
					while ((line = reader.readLine()) != null) {
						mResult = mResult + line;
					}
					Log.d("PRQR", "Result"+mResult);
					JSONObject jsonObject = new JSONObject(mResult.substring(4,mResult.length()));
					status_code = jsonObject.getString("status_code");
					mMessage = jsonObject.getString("message");
				} catch (Exception e) {
					Log.d("QRPR", e.toString());
				}
				if(status_code.equals("200")) {
					isSuccess = true;
				} else {
					isSuccess = false;
					Log.e("PRQR", "status code error");
				}
			} catch (Exception e) {
				isSuccess = false;
				Log.e("PRQR", e.toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String string) {
			super.onPostExecute(string);
			if(isSuccess) {
				Toast.makeText(QRActivity.this, "Scan successful", Toast.LENGTH_SHORT).show();
				if(mMessage!=null) {
					mQrMessageView.setText(mMessage);
				}
			}
			else {
				if(mMessage!=null) {
					mQrMessageView.setText(mMessage);
				}
				if(string!=null && string.equals("Same"))
					Toast.makeText(QRActivity.this, "You scanned admin qr instead of client", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(QRActivity.this, "Scan failed", Toast.LENGTH_SHORT).show();
			}
		}
	}
}
