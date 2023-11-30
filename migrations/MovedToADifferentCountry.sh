#!/bin/bash

echo ""
echo "Applying migration MovedToADifferentCountry"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /movedToADifferentCountry                        controllers.MovedToADifferentCountryController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /movedToADifferentCountry                        controllers.MovedToADifferentCountryController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeMovedToADifferentCountry                  controllers.MovedToADifferentCountryController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeMovedToADifferentCountry                  controllers.MovedToADifferentCountryController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "movedToADifferentCountry.title = movedToADifferentCountry" >> ../conf/messages.en
echo "movedToADifferentCountry.heading = movedToADifferentCountry" >> ../conf/messages.en
echo "movedToADifferentCountry.checkYourAnswersLabel = movedToADifferentCountry" >> ../conf/messages.en
echo "movedToADifferentCountry.error.required = Select yes if movedToADifferentCountry" >> ../conf/messages.en
echo "movedToADifferentCountry.change.hidden = MovedToADifferentCountry" >> ../conf/messages.en

echo "Migration MovedToADifferentCountry completed"
