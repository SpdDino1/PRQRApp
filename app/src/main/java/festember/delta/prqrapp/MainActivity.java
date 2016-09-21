package festember.delta.prqrapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

	private Button mQRButton, mLogoutButton;
	private SharedPreferences mSharedPreferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mQRButton = (Button) findViewById(R.id.button_scan_qr);
		mSharedPreferences = getSharedPreferences("AdminDetails",
			Context.MODE_PRIVATE);
		mQRButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this, QRActivity.class);
				startActivity(intent);
			}
		});
		mLogoutButton = (Button) findViewById(R.id.button_logout);
		mLogoutButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View view) {
				SharedPreferences.Editor sharedPredEdit = mSharedPreferences.edit();
				sharedPredEdit.clear();
				sharedPredEdit.apply();
				MainActivity.this.recreate();
			}
		});
	}
}
