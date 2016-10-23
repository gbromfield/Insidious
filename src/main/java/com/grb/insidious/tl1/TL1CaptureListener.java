package com.grb.insidious.tl1;

import com.grb.tl1.TL1OutputMessage;

public interface TL1CaptureListener {
	public void onTL1Output(TL1OutputMessage outputMsg);
}
