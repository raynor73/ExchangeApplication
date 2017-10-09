package ru.ilapin.exchangeapplication.backend;

public class Result<T> {

	private T mData;
	private boolean mHasError;
	private boolean mIsEmpty;

	public Result() {}

	public Result(final T data, final boolean hasError, final boolean isEmpty) {
		mData = data;
		mHasError = hasError;
		mIsEmpty = isEmpty;
	}

	public T getData() {
		return mData;
	}

	public void setData(final T data) {
		mData = data;
	}

	public boolean hasError() {
		return mHasError;
	}

	public void setHasError(final boolean hasError) {
		mHasError = hasError;
	}

	public boolean isEmpty() {
		return mIsEmpty;
	}

	public void setEmpty(final boolean empty) {
		mIsEmpty = empty;
	}
}
