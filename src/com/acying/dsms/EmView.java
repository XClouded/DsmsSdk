package com.acying.dsms;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

public interface EmView extends DSmsdt{
	
	public void init(Context ctx);
	public View getView();
	public void setExtras(Bundle extras);
}
