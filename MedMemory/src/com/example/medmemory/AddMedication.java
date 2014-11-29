package com.example.medmemory;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.medmemory.db.Database;
import com.example.medmemory.model.Medication;

public class AddMedication extends Activity {

	private static final int SELECT_PICTURE = 1;
	public static final String MEDICATION_ID = "MEDICATION_ID";
	
	TextView medName;
	TextView dosage;
	TextView pillCount;
	TextView notes;
	
	TimePicker timePicker;
	
	ImageView imageView;

	Button saveBtn;
	Button selectImageBtn;
	Button testNotificationBtn;
	
	Date reminderDate;
	
	Bitmap image;
	
	Medication editMed;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addmedication);
        setTitle("Add Medication");
        
        medName = (TextView) findViewById(R.id.med_name);
        dosage = (TextView) findViewById(R.id.med_dosage);
        pillCount = (TextView) findViewById(R.id.med_count);
        notes = (TextView) findViewById(R.id.med_notes);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        imageView = (ImageView) findViewById(R.id.med_image);
        
        saveBtn = (Button) findViewById(R.id.save_med_btn);
        saveBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				// Default data for testing purposes
				if(medName.getText().length() == 0)
					medName.setText("Aspirin");
				if(dosage.getText().length() == 0)
					dosage.setText("100mg");
				if(pillCount.getText().length() == 0)
					pillCount.setText("30");
				if(notes.getText().length() == 0)
					notes.setText("Take with food, do not take with orange juice.");
				
				// Debug log
				System.out.println("====SAVING MED====");
				System.out.println("Name: "+medName.getText());
				System.out.println("Dosage: "+dosage.getText());
				System.out.println("Pill Count: "+pillCount.getText());
				System.out.println("Notes: "+notes.getText());
				System.out.println("Reminder Time: "+timePicker.getCurrentHour()+":"+timePicker.getCurrentMinute());
				
				if(editMed == null)
				{
					// Create medication
					Medication med = new Medication();
					med.setName(medName.getText().toString());
					med.setImage(image);
					med.setDosage(dosage.getText().toString());
					med.setCurrentPillCount(Integer.parseInt(pillCount.getText().toString()));
					med.setMaximumPillCount(Integer.parseInt(pillCount.getText().toString()));
					med.setNotes(notes.getText().toString());
					
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.HOUR_OF_DAY,timePicker.getCurrentHour());
					cal.set(Calendar.MINUTE,timePicker.getCurrentMinute());
					cal.set(Calendar.SECOND,0);
					cal.set(Calendar.MILLISECOND,0);
					med.setReminderDate(cal.getTime());
					
					Database.context = AddMedication.this;
					boolean success = Database.addMedication(med);
					
					// Set the alarm.
					int medId = Database.getLastInsertId();
					setMedAlarm(medId, cal, med.getName(), med.getImage(), med.getDosage(), med.getNotes());
					
					System.out.println("Added med successfully? "+success);
				}
				else
				{	
					// Update editMed:
					editMed.setName(medName.getText().toString());
					editMed.setImage(image);
					editMed.setDosage(dosage.getText().toString());
					editMed.setCurrentPillCount(Integer.parseInt(pillCount.getText().toString()));
					editMed.setMaximumPillCount(Integer.parseInt(pillCount.getText().toString()));
					editMed.setNotes(notes.getText().toString());
					
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.HOUR_OF_DAY,timePicker.getCurrentHour());
					cal.set(Calendar.MINUTE,timePicker.getCurrentMinute());
					cal.set(Calendar.SECOND,0);
					cal.set(Calendar.MILLISECOND,0);
					editMed.setReminderDate(cal.getTime());

					Database.context = AddMedication.this;
					boolean success = Database.updateMedication(editMed);
					
					int medId = Database.getLastInsertId();
					setMedAlarm(medId, cal, editMed.getName(), editMed.getImage(), editMed.getDosage(), editMed.getNotes());
					System.out.println("Added med successfully? "+success);
				}
				
				// Add to DB
				
				
				setResult(RESULT_OK);
				finish();
			}
		});
        
        selectImageBtn = (Button) findViewById(R.id.select_image_btn);
        selectImageBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				Intent pickIntent = new Intent();
				pickIntent.setType("image/*");
				pickIntent.setAction(Intent.ACTION_GET_CONTENT);

				Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

				String pickTitle = "Select Image";
				Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
				chooserIntent.putExtra
				(
				  Intent.EXTRA_INITIAL_INTENTS, 
				  new Intent[] { takePhotoIntent }
				);

				startActivityForResult(chooserIntent, SELECT_PICTURE);
			}
		});
        
        // Test notification button listener.
        testNotificationBtn = (Button) findViewById(R.id.test_notification_button);
        testNotificationBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				// Add notification //////////
				// Create items for a "TakeMedNow" action.
				Intent takeMedNowIntent = new Intent(AddMedication.this , TakeMedNowService.class);     
				PendingIntent takeMedNowPendingIntent = PendingIntent.getService(getApplicationContext(), 0, takeMedNowIntent, 0);
				String takeMedNowTitle = "Take Now";
				int takeMedNowIcon = R.drawable.medical87;
				
				// Create items for a "Snooze" action.
				Intent snoozeIntent = new Intent(AddMedication.this , TakeMedNowService.class);     
				PendingIntent snoozePendingIntent = PendingIntent.getService(getApplicationContext(), 0, snoozeIntent, 0);
				String snoozeTitle = "Snooze";
				int snoozeIcon = R.drawable.man322;
				
				
				Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				
				NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AddMedication.this);
				
				// Add the "TakeMedNow" and "Snooze" actions.
				mBuilder.addAction(takeMedNowIcon, takeMedNowTitle, takeMedNowPendingIntent);
				mBuilder.addAction(snoozeIcon, snoozeTitle, snoozePendingIntent);
				
				
				mBuilder.setSmallIcon(R.drawable.noticon);
				mBuilder.setLargeIcon(image);
				mBuilder.setContentTitle("Take 100mg of Aspirin");
				mBuilder.setContentText("Take with food, do not take with orange juice.");
				mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Take with food, do not take with orange juice."));
				mBuilder.setTicker("It's time to take your medication");
				mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
				mBuilder.setSound(alarmSound);
				mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = mBuilder.build();
				notification.flags = Notification.FLAG_ONGOING_EVENT;
				mNotificationManager.notify(1, notification);
			}	
        });
        
        Intent intent = getIntent();
        int medId = intent.getIntExtra(MEDICATION_ID, -1);
        if(medId != -1)
        {
        	Database.context = this;
        	editMed = Database.getMedicationById(medId);
			medName.setText(editMed.getName());
			dosage.setText(editMed.getDosage());
			pillCount.setText(""+editMed.getCurrentPillCount()); 
			notes.setText(editMed.getNotes());
			image = editMed.getImage();
			imageView.setImageBitmap(editMed.getImage());
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(editMed.getReminderDate());
			timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
			timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
        }
        else
        {
        	AssetManager assetManager = getAssets();
        	InputStream in;
        	try {
        		in = assetManager.open("default_med_pic.jpg");
        		image = BitmapFactory.decodeStream(in);
        		imageView.setImageBitmap(image);
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == SELECT_PICTURE && resultCode == RESULT_OK)
		{
			if(data.getData() != null) // Gallery returned data
			{
				Uri selectedImage = data.getData();
				
				
				ParcelFileDescriptor parcelFD;
				try
				{
					parcelFD = getContentResolver().openFileDescriptor(selectedImage, "r");
					FileDescriptor fd = parcelFD.getFileDescriptor();
					image = BitmapFactory.decodeFileDescriptor(fd);
				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
			else // Camera returned data
			{
				Bundle extras = data.getExtras();
				image = (Bitmap) extras.get("data");
			}
			
			imageView.setImageBitmap(image);
		}
	    
	}
	
	
	private void setMedAlarm(int medId, Calendar cal, String name, Bitmap image, String dosage, String notes) {
		// **DEBUG** Log the medId
		System.out.println("medId=" + medId);
		
		// Create the intent that will display the notification.
		Intent intent = new Intent(AddMedication.this , NotifyService.class);
		
		// Add the data needed for the notification.
		intent.putExtra("medId", medId);
		intent.putExtra("name", name);
		intent.putExtra("image", image);
		intent.putExtra("dosage", dosage);
		intent.putExtra("notes", notes);
		
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
	
	
}
