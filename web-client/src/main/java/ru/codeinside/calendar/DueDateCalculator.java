/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2014, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.calendar;

import java.util.Date;

/**
 * Поиск даты окончания периода
 */
public abstract class DueDateCalculator {
  public abstract Date calculate(Date startDate, int countDays);
}
