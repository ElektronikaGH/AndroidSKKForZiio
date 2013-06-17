package jp.elektronika.AndroidSKKForZiio;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;


public class SKKDictDir extends ListActivity {
	public static final String ACTION_BROADCAST ="jp.elektronika.AndroidSKKForZiio.DICTOOL_ACTION_SEND";
	public static final String CATEGORY_BROADCAST = "jp.elektronika.AndroidSKKForZiio.DICTOOL_VALUE";
	public static final String VALUE_KEY = "VALUE";
	private static final int REQUEST_DIC = 0;
	private String mDictDir = "/mnt/sdcard/";

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dictool);
		mDictDir = SKKPrefs.getPrefDictDir(getBaseContext());

		openFileActivity(REQUEST_DIC);
	}

	private void openFileActivity(int requestCode) {
		Intent intent = new Intent(SKKDictDir.this, FileChooser.class);
		intent.putExtra(FileChooser.KEY_MODE, FileChooser.MODE_OPEN);
		intent.putExtra(FileChooser.KEY_DIRNAME, mDictDir);
		startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		String str = null;

		switch (requestCode) {
		case REQUEST_DIC:
			if (resultCode == RESULT_OK) {
				str = intent.getStringExtra(FileChooser.KEY_DIRNAME);
				if (str == null) return;
				SKKPrefs.setPrefDictDir(this, str);
				Intent retIntent = new Intent(SKKDictDir.ACTION_BROADCAST);
				retIntent.addCategory(SKKDictDir.CATEGORY_BROADCAST);
				retIntent.putExtra(SKKDictDir.VALUE_KEY, "DICT_REOPEN");
				sendBroadcast(retIntent);
			}
			break;
		default:
			break;
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override public void onPause() {
		super.onPause();
	}
}
