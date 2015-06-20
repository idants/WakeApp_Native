package com.example.wakeapp;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;

public class EditTextLocker {
    private EditText editText;
    private int min;
    private int max;
    private int charactersLimit;
    private EditText nextEditText;

    public EditTextLocker(EditText editText, int min, int max, EditText nextEditText) {
        this.editText = editText;
        this.min = min;
        this.max= max;
        
        if (nextEditText != null){
        	this.nextEditText = nextEditText;
        }
    }
    
    private void checkLimits(boolean requestFocus) {
    	String editTextValue = editText.getText().toString().trim();
        if (editTextValue.equalsIgnoreCase("")) {
        	startStopEditing(false);
        	return;
        }
        
        int editTextLength = editTextValue.length();
        
        boolean doLock = editTextLength >= charactersLimit;
        startStopEditing(doLock);
        if (requestFocus && doLock && nextEditText != null){
        	nextEditText.requestFocus();
        }
    }

    private TextWatcher editTextWatcherForCharacterLimits = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        	if (s.length() == 1){
        		checkLimits(true);
        		if (s.charAt(0) == '0' && before == 1){
        			editText.removeTextChangedListener(editTextWatcherForCharacterLimits);
                	editText.setText("");
                	editText.addTextChangedListener(editTextWatcherForCharacterLimits);	
        		}
        		else if (s.charAt(0) != '0'){
        			editText.removeTextChangedListener(editTextWatcherForCharacterLimits);
                	editText.setText("0" + editText.getText());
                	editText.setSelection(2);
                	editText.addTextChangedListener(editTextWatcherForCharacterLimits);	
        		}
        	}
        	else if (s.length() == 2){
        		checkLimits(true);
        	}
        	else if (s.length() == 3){
        		if (s.toString().contains("0")){
            		editText.removeTextChangedListener(editTextWatcherForCharacterLimits);
                	editText.setText(s.toString().replace("0", ""));
                	editText.setSelection(2);
                	editText.addTextChangedListener(editTextWatcherForCharacterLimits);
        		}
            	checkLimits(true);
        	}
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    };

    public void limitCharacters(final int limit) {
        this.charactersLimit = limit;
        editText.addTextChangedListener(editTextWatcherForCharacterLimits);
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {          
            public void onFocusChange(View v, boolean hasFocus) {
            	checkLimits(false);
            }
        });
    }

    public void unlockEditText() {
        startStopEditing(false);
    }
    
    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
    
    private CharSequence checkMinMax(CharSequence source, CharSequence dest){
    	try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException nfe) { }     
        return "";
    }

    public void startStopEditing(boolean isLock) {
        if (isLock) {
            editText.setFilters(new InputFilter[] { new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                	if (start == dstart && end == 1 && dend == 2){ // this is for when the user select all input text (2 digits) and replaces it with a single digit
                		return source;
                	}
                	else{
                		return source.length() < 1 ? dest.subSequence(dstart, dend) : "";	
                	}
                }
            } });
        } else {
            editText.setFilters(new InputFilter[] { new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                	if (start == dstart && end == 1 && dend == 2){ //this is for when the user selects input text and types a number to replace it
                		return checkMinMax("", dest);
                	}
                	else{
                		return checkMinMax(source, dest);
                	}
                }
            } });
        }
    }
}
