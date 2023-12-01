#!/bin/bash

echo ""
echo "Applying migration EuCountry"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /euCountry                        controllers.EuCountryController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /euCountry                        controllers.EuCountryController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeEuCountry                  controllers.EuCountryController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeEuCountry                  controllers.EuCountryController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "euCountry.title = euCountry" >> ../conf/messages.en
echo "euCountry.heading = euCountry" >> ../conf/messages.en
echo "euCountry.checkYourAnswersLabel = euCountry" >> ../conf/messages.en
echo "euCountry.error.required = Select yes if euCountry" >> ../conf/messages.en
echo "euCountry.change.hidden = EuCountry" >> ../conf/messages.en

echo "Migration EuCountry completed"
