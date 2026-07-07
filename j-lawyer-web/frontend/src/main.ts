import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// German number/date formatting (LOCALE_ID is set in app.config).
registerLocaleData(localeDe);

bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
