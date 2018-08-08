package org.mtransit.android.commons;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TimeUtilsTests {

	@Test
	public void testCleanTimes() {
		String input;
		SpannableStringBuilder output;
		//
		input = "2:06 am";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		TimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(7), anyInt());
		//
		input = "2:06 pm";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		TimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(7), anyInt());
		//
		input = "2:06 a.m.";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		TimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(9), anyInt());
		//
		input = "2:06 p.m.";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		TimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(9), anyInt());
	}
}
