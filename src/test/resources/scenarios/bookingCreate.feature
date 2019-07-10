Feature: TCV -> MSD (Feature 3 - booking create)

  Background: Create customer for test
    Given msd-manage-customer ms sent customer create /request/msdManageCustomer-MSD.json
    Then validate and verify customer /response/customer/MSD_customer.json according to schema

  Scenario Outline: TCV request for created booking is successfully processed
    Given TCV sent booking create /request/TCV-tcvUpdateBooking.json with <BOOKING_INITIATING_SYSTEM_ID>
    When validate and verify booking <MSD_BOOKING_RESPONSE> according to schema
    Then execute /request/kibanaBooking.json


    Examples:
      | BOOKING_INITIATING_SYSTEM_ID | MSD_BOOKING_RESPONSE              |
      | Atcore                       | /response/booking/MSD_Atcore.json |
      | WebRIO                       | /response/booking/MSD_WebRIO.json |

