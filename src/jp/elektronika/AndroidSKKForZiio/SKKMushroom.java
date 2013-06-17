package jp.elektronika.AndroidSKKForZiio;

import jp.elektronika.AndroidSKKForZiio.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class SKKMushroom extends Activity {
	// ---- simeji mushroom
	public static final String ACTION_SIMEJI_MUSHROOM = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	public static final String CATEGORY_SIMEJI_MUSHROOM = "com.adamrocker.android.simeji.REPLACE";

	public static final String ACTION_BROADCAST = "jp.elektronika.AndroidSKKForZiio.MUSHROOM_SEND";
	public static final String CATEGORY_BROADCAST = "jp.elektronika.AndroidSKKForZiio.MUSHROOM_VALUE";

	public static final String REPLACE_KEY = "replace_key";


	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);

		// ---- simeji mushroom
		try {
			Intent mushroom = new Intent(ACTION_SIMEJI_MUSHROOM);
			mushroom.addCategory(CATEGORY_SIMEJI_MUSHROOM);

			Bundle extras = getIntent().getExtras();
			String s = extras.getString(REPLACE_KEY);
			mushroom.putExtra(REPLACE_KEY, s);

			//      Toast toast = Toast.makeText(getApplicationContext(),s , Toast.LENGTH_SHORT );
			//      toast.show();

			startActivityForResult(mushroom, 1);  
		} catch (ActivityNotFoundException e) {
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String s = extras.getString(REPLACE_KEY);

			//  		Toast toast = Toast.makeText(getApplicationContext(),s , Toast.LENGTH_SHORT );
			//        toast.show();

			Intent retIntent = new Intent(ACTION_BROADCAST);
			retIntent.addCategory(CATEGORY_BROADCAST);
			retIntent.putExtra(REPLACE_KEY, s);
			sendBroadcast(retIntent);        
		}
		finish();
	}
}
