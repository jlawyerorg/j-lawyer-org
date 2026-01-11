-- Create table for historical base interest rates (Basiszinssatz)
CREATE TABLE interest_base (
    id VARCHAR(50) NOT NULL,
    valid_from DATE NOT NULL,
    rate DECIMAL(5,2) NOT NULL,
    source VARCHAR(100),
    PRIMARY KEY (id),
    INDEX idx_valid_from (valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Insert historical German base interest rates from 2002 onwards
-- Source: Deutsche Bundesbank (§ 247 BGB Basiszinssatz)
-- Rates are valid from the specified date until the next change

INSERT INTO interest_base (id, valid_from, rate, source) VALUES
('bi-2002-01-01', '2002-01-01', 2.57, 'Deutsche Bundesbank'),
('bi-2002-07-01', '2002-07-01', 2.47, 'Deutsche Bundesbank'),
('bi-2003-01-01', '2003-01-01', 1.97, 'Deutsche Bundesbank'),
('bi-2003-07-01', '2003-07-01', 1.22, 'Deutsche Bundesbank'),
('bi-2004-01-01', '2004-01-01', 1.14, 'Deutsche Bundesbank'),
('bi-2004-07-01', '2004-07-01', 1.13, 'Deutsche Bundesbank'),
('bi-2005-01-01', '2005-01-01', 1.21, 'Deutsche Bundesbank'),
('bi-2005-07-01', '2005-07-01', 1.17, 'Deutsche Bundesbank'),
('bi-2006-01-01', '2006-01-01', 1.37, 'Deutsche Bundesbank'),
('bi-2006-07-01', '2006-07-01', 1.95, 'Deutsche Bundesbank'),
('bi-2007-01-01', '2007-01-01', 2.70, 'Deutsche Bundesbank'),
('bi-2007-07-01', '2007-07-01', 3.19, 'Deutsche Bundesbank'),
('bi-2008-01-01', '2008-01-01', 3.32, 'Deutsche Bundesbank'),
('bi-2008-07-01', '2008-07-01', 3.19, 'Deutsche Bundesbank'),
('bi-2009-01-01', '2009-01-01', 1.62, 'Deutsche Bundesbank'),
('bi-2009-07-01', '2009-07-01', 0.12, 'Deutsche Bundesbank'),
('bi-2010-01-01', '2010-01-01', 0.12, 'Deutsche Bundesbank'),
('bi-2010-07-01', '2010-07-01', 0.12, 'Deutsche Bundesbank'),
('bi-2011-01-01', '2011-01-01', 0.12, 'Deutsche Bundesbank'),
('bi-2011-07-01', '2011-07-01', 0.37, 'Deutsche Bundesbank'),
('bi-2012-01-01', '2012-01-01', 0.12, 'Deutsche Bundesbank'),
('bi-2012-07-01', '2012-07-01', 0.12, 'Deutsche Bundesbank'),
('bi-2013-01-01', '2013-01-01', -0.13, 'Deutsche Bundesbank'),
('bi-2013-07-01', '2013-07-01', -0.38, 'Deutsche Bundesbank'),
('bi-2014-01-01', '2014-01-01', -0.63, 'Deutsche Bundesbank'),
('bi-2014-07-01', '2014-07-01', -0.73, 'Deutsche Bundesbank'),
('bi-2014-07-29', '2014-07-29', -0.73, 'Deutsche Bundesbank'),
('bi-2015-01-01', '2015-01-01', -0.83, 'Deutsche Bundesbank'),
('bi-2015-07-01', '2015-07-01', -0.83, 'Deutsche Bundesbank'),
('bi-2016-01-01', '2016-01-01', -0.83, 'Deutsche Bundesbank'),
('bi-2016-07-01', '2016-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2017-01-01', '2017-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2017-07-01', '2017-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2018-01-01', '2018-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2018-07-01', '2018-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2019-01-01', '2019-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2019-07-01', '2019-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2020-01-01', '2020-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2020-07-01', '2020-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2021-01-01', '2021-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2021-07-01', '2021-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2022-01-01', '2022-01-01', -0.88, 'Deutsche Bundesbank'),
('bi-2022-07-01', '2022-07-01', -0.88, 'Deutsche Bundesbank'),
('bi-2023-01-01', '2023-01-01', 1.62, 'Deutsche Bundesbank'),
('bi-2023-07-01', '2023-07-01', 3.12, 'Deutsche Bundesbank'),
('bi-2024-01-01', '2024-01-01', 3.62, 'Deutsche Bundesbank'),
('bi-2024-07-01', '2024-07-01', 3.37, 'Deutsche Bundesbank'),
('bi-2025-01-01', '2025-01-01', 2.27, 'Deutsche Bundesbank'),
('bi-2025-07-01', '2025-07-01', 1.27, 'Deutsche Bundesbank');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.4.0.12') ON DUPLICATE KEY UPDATE settingValue = '3.4.0.12';
commit;
