package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Customer;

import java.time.LocalDate;
import java.time.Period;

public record CustomerItem(Long id, String fullName, String mobile, int age) {
    public static CustomerItem from(Customer c) {
        return new CustomerItem(c.getCustomerId(), c.getFullName(), c.getMobile(),
                Period.between(c.getBirthday(), LocalDate.now()).getYears());
    }

    @Override
    public String toString() {
        return fullName + " – " + mobile + " – " + age + " tuổi";
    }
}

