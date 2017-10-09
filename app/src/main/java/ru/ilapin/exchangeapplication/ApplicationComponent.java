package ru.ilapin.exchangeapplication;

import dagger.Component;
import ru.ilapin.exchangeapplication.backend.BackendModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {SystemModule.class, BackendModule.class})
public interface ApplicationComponent {

	void inject(MainActivity activity);
}
