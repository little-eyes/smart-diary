/**
 *
 * NFC is involved to detect if two people meet,
 * this required optional gesture to put phones
 * together. This small gesture can inject the
 * diary a very important information.
 * 
 * The detected people will be stored in CommonData,
 * then broadcast an intent to inform generator.
 * 
 **/

package com.smartdiary;

import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;

public class NfcMessenger {
	
	private NfcAdapter mNfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private IntentFilter[] mNdefExchangeFilters;
	private String[][] mTechLists;
    private NdefMessage mMessage;

    static Charset UTFENCODING = Charset.forName("UTF-8");
    
    // Acquire the NFC Adapter and set up for sending and receiving NDEF messages
    NfcMessenger(Context context)
    {
    	mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        mNfcPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try
        {
            ndefDetected.addDataType("text/plain");
        }
        catch (MalformedMimeTypeException e)
        {
        }
        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };
        mTechLists = new String[][] { new String[] { context.getClass().getName() } };
    }
    
    // Set the current NDEF message
    public void setMessage(Activity activity, String msg)
    {
    	NdefRecord ndefrecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], msg.getBytes(UTFENCODING));
    	mMessage = new NdefMessage(new NdefRecord[] { ndefrecord });
    	mNfcAdapter.enableForegroundNdefPush(activity, mMessage);
    }
    
    // Check if an intent is an NDEF message and if so parse it
    public String checkNfcMessage(Intent intent)
    {
    	String text = null;
    	if (intent.getAction().contentEquals(NfcAdapter.ACTION_NDEF_DISCOVERED))
    	{
    		NdefMessage msg = (NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
    		
    		NdefRecord record = msg.getRecords()[0];
    		byte[] data = record.getPayload();
    		text = new String(data, UTFENCODING);
    	}
    	return text;
    }
    
    // Enable sending and receiving NFC messages
    public void enableNfc(Activity activity)
    {
    	if (mMessage != null)
    		mNfcAdapter.enableForegroundNdefPush(activity, mMessage);
    	mNfcAdapter.enableForegroundDispatch(activity, mNfcPendingIntent, mNdefExchangeFilters, mTechLists);
    }
    
    // Disable sending and receiving NFC messages
    public void disableNfc(Activity activity)
    {
    	mNfcAdapter.disableForegroundNdefPush(activity);
        mNfcAdapter.disableForegroundDispatch(activity);
    }
}
